properties([parameters([string(description: 'Recipe reference that has changed', name: 'name_version', defaultValue: 'LIB_A/1.0'),
                        string(description: 'User/Channel', name: 'user_channel', defaultValue: 'lasote/stable'),
                        string(description: 'Config repository URL', name: 'conf_repo_url', defaultValue: 'https://github.com/lasote/skynet_example.git'),
                        string(description: 'Config repository branch', name: 'conf_repo_branch', defaultValue: 'master'),
                        ])])

def get_build_order_for_leave(ref, leave_name, profiles, repo_branch, repo_url, recipe_dir, client, conf_repo_dir) {
    def profiles_bo = [:]
    dir(leave_name + "_repo"){
        echo "Getting leave recipe '${leave_name}' ${repo_branch} ${repo_url}"
        git branch: repo_branch, url: repo_url
        dir(recipe_dir){
            for(profile in profiles){
                profiles_bo[profile] = []
                client.run(command: "info . -bo " + ref + " --json bo.json --profile \"" + conf_repo_dir + "/" + profile+ "\"")
                def bo_json = readJSON file: "./bo.json"
                profiles_bo[profile].addAll(bo_json["groups"])
                echo "Build order for recipe '${ref}' is " + bo_json
            }
        }
    }

    // profiles_bo:
    //   {./profiles/osx_64:[[zlib/1.2.8@lasote/stable], [OpenSSL/1.0.2i@lasote/stable]],
    //    ./profiles/osx_32:[[zlib/1.2.8@lasote/stable], [OpenSSL/1.0.2i@lasote/stable]]}
    return profiles_bo
}

def get_tasks_groups(profiles_bo){
    // Merge all the profiles groups to run in parallel, first group from profile1 with first group from profile2, second
    // with second and so on
    def tasks_groups = []
    def group_index = 0
    while(true){
        tasks = []
        profiles_bo = entries(profiles_bo)
        for (int i = 0; i < profiles_bo.size(); i++) {
            def profile = profiles_bo[i]
            if(profile[1].size() <= group_index){
                continue;
            }
            for(ref in profile[1][group_index]){
                string name_p = ref.split("@")[0]
                string channel = ref.split("@")[1].split("/")[1]
                string task_name = ref + "_" + profile[0]
                string prof_name = profile[0].split("/").last()
                tasks.add([build_label: ref + " (${prof_name})",
                           ref: ref,
                           name_version: name_p,
                           channel: channel,
                           profile: profile[0]])
            }
        }
        if(tasks.size() == 0){
           break
        }
        tasks_groups.add(tasks)
        group_index += 1
    }
    return tasks_groups
}


def launch_task_group(tasks_groups, conf_repo_url, conf_repo_branch){
    // Runs the tasks parallelizing
    for(int i=0; i<tasks_groups.size(); i++){
        tasks = [:]
        for(int j=0; j<tasks_groups[i].size(); j++){
            def a_build = tasks_groups[i][j]
            def label = a_build["build_label"]
            echo "BUILD: ${a_build}"
            tasks[label] = { -> build(job: "conan-single-build",
                              parameters: [
                                 string(name: "build_label", value: label),
                                 string(name: "channel", value: a_build["channel"]),
                                 string(name: "name_version", value: a_build["name_version"]),
                                 string(name: "conf_repo_url", value: conf_repo_url),
                                 string(name: "conf_repo_branch", value: conf_repo_branch),
                                 string(name: "profile", value: a_build["profile"])
                              ]
                       )
            }
        }
        parallel(tasks)
    }
}

@NonCPS def entries(m) {m.collect {k, v -> [k, v]}}


node {
     withEnv(['PATH+JENKINSHOME=/usr/local/bin']) {
    currentBuild.displayName = "Rebuild of " + params.name_version
    def data
    def conf_repo_dir
    def client

    stage("Configure/Get repositories"){
        dir("_conf_repo"){
            git branch: params.conf_repo_branch, url: params.conf_repo_url
            data = readYaml file: "conan_ci_conf.yml"
            conf_repo_dir = pwd()
            artifactory = data.artifactory
        }
        def server = Artifactory.server data.artifactory.name
        client = Artifactory.newConanClient()
        def serverName = client.remote.add server: server, repo: data.artifactory.repo
        //client.run(command: "remote remove conan.io")
    }

    stage("Fire deps"){
        def ref = params.name_version + "@" + params.user_channel
        def user = params.user_channel.split("/")[0]
        def channel = params.user_channel.split("/")[1]
        def leaves = entries(data.leaves)
        withEnv(["CONAN_USERNAME=${user}", "CONAN_CHANNEL=${channel}"]){
            for (int i = 0; i < leaves.size(); i++) {
                def leave = leaves.get(i)
                def leave_name = leave[0];
                def profiles = leave[1]["profiles"];
                def repo = data.repos.get(leave_name)
                profiles_bo = get_build_order_for_leave(ref, leave_name, profiles, repo.branch,
                                                        repo.url, repo.dir, client,
                                                        conf_repo_dir)
                def tasks_groups = get_tasks_groups(profiles_bo)
                echo "Running in parallel: ${tasks_groups}"
                launch_task_group(tasks_groups, params.conf_repo_url, params.conf_repo_branch)
            }
        }
    }
     }
}
