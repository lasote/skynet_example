properties([parameters([string(description: 'Build label', name: 'build_label', defaultValue: 'Unamed'),
                        string(description: 'Channel', name: 'channel', defaultValue: 'stable'),
                        string(description: 'Name/Version', name: 'name_version', defaultValue: 'LIB_A/1.0'),
                        string(description: 'Profile', name: 'profile', defaultValue: './profiles/64bits'),
                        string(description: 'Config repository branch', name: 'conf_repo_branch', defaultValue: 'master'),
                        string(description: 'Config repository url', name: 'conf_repo_url', defaultValue: 'https://github.com/lasote/skynet_example.git'),
                       ])])
String docker_image = 'lasote/conangcc6-armv7'

node {
    
    currentBuild.displayName = params.build_label
    def buildInfo
    def data
    def conf_repo_dir
    def client
    def serverName
    withEnv(['PATH+JENKINSHOME=/usr/local/bin']) {
            def server
        stage("Configure/Get repositories"){

            dir("_conf_repo"){
                git branch: params.conf_repo_branch, url: params.conf_repo_url
                data = readYaml file: "conan_ci_conf.yml"
                conf_repo_dir = pwd()
            }
            server = Artifactory.server data.artifactory.name
            client = Artifactory.newConanClient()
            
            serverName = client.remote.add server: server, repo: data.artifactory.repo
            //client.run(command: "remote remove conan.io")

            dir("_lib_repo"){
                git branch: data.repos[params.name_version].branch, url: data.repos[params.name_version].url
            }
        }
        stage("Build packages"){
            // For each profile
            dir("_lib_repo"){
                dir(data.repos[params.name_version].dir){
                    client.run(command: "create . lasote/stable -pr \"" + conf_repo_dir + "/" + params.profile + "\"")
                }
            }
        }
        
        stage("Upload Artifactory"){
            String command = "upload * -r ${serverName} --all -c"
            buildInfo = client.run(command: command)
            buildInfo.env.collect()
            server.publishBuildInfo buildInfo
        }
        
        stage("Test"){
               
        }
        
        stage("Promote"){
            def promotionConfig = [
            // Mandatory parameters
            'buildName'          : buildInfo.name,
            'buildNumber'        : buildInfo.number,
            'targetRepo'         : 'conan-prod-local',
    
            // Optional parameters
            'comment'            : 'ready for prod',
            'sourceRepo'         : 'conan-dev-local',
            'status'             : 'Released',
            'includeDependencies': true,
            'copy'               : false
        ]

        // Promote build
        server.promote promotionConfig   
        
        }
        
    }
}
