import time
from conans import ConanFile

class ProjectConan(ConanFile):
    license = "MIT"
    url = "https://github.com/lasote/skynet_example"
    settings = "os", "compiler", "build_type", "arch"

    def requirements(self):
        self.requires("LIB_D/1.0@%s/%s" % (self.user, self.channel))
        self.requires("LIB_G/1.0@%s/%s" % (self.user, self.channel))

    def build(self):
        self.output.warn("Building project...")
        time.sleep(2)
