import os

for lib in ["A", "B", "C", "D", "E", "F", "G"]:
    os.system('cd recipes/%s && conan test_package && conan upload "LIB_%s/*" -r artifactory -c ' % (lib, lib))