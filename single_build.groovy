


properties([parameters([string(description: 'Build label', name: 'build_label', defaultValue: 'Unamed'),
                        string(description: 'Channel', name: 'channel', defaultValue: 'stable'),
                        string(description: 'Name/Version', name: 'name_version', defaultValue: 'zlib/1.2.8'),
                        string(description: 'Profile', name: 'profile', defaultValue: './profiles/osx_64'),
                        string(description: 'Config repository branch', name: 'conf_repo_branch', defaultValue: 'master'),
                        string(description: 'Config repository url', name: 'conf_repo_url', defaultValue: 'https://github.com/lasote/conf_jenkins_test.git'),
                       ])])


node {
    currentBuild.displayName = params.build_label
    def data
    def conf_repo_dir
    def client
    def serverName

    stage("Configure/Get repositories"){
        dir("_conf_repo"){
            git branch: params.conf_repo_branch, url: params.conf_repo_url
            data = readYaml file: "conan_ci_conf.yml"
            conf_repo_dir = pwd()
        }
        def server = Artifactory.server data.artifactory.name
        client = Artifactory.newConanClient()
        serverName = client.remote.add server: server, repo: data.artifactory.repo
        client.run(command: "remote remove conan.io")

        dir("_lib_repo"){
            git branch: data.repos[params.name_version].branch, url: data.repos[params.name_version].url
        }
    }
    stage("Build packages"){
        // For each profile
        dir("_lib_repo"){
            dir(data.repos[params.name_version].dir){
                client.run(command: "test_package --update --profile " + conf_repo_dir + "/" + params.profile)
            }
        }
    }
    
    stage("Upload Artifactory"){
        String command = "upload * -r ${serverName} --all -c"
        client.run(command: command)
    }
}

