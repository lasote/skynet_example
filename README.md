

- Install in Jenkins the "Pipeline Utility Steps": plugin, which is necessary to parse Yaml

```
$ git clone <this repo> && cd skynet_example
$ conan remote add artifactory <your artifactory url>
$ python upload_recipes.py # This assumes your artifactory remote is called "artifactory"
```

- In Jenkins Artifactory plugin configuration, ensure that the instance is called ``artifactory``
- Create a "SimpleBuild" pipeline in Jenkins with the contents of ``single_build.groovy``
- Create a "MultiBuild" pipeline in Jenkins with contents of ``multi_build.groovy``


