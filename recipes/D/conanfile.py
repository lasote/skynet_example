import time
from conans import ConanFile

class DConan(ConanFile):
    name = "LIB_D"
    version = "1.0"
    license = "MIT"
    url = "https://github.com/lasote/skynet_example"
    settings = "os", "compiler", "build_type", "arch"

    def requirements(self):
        self.requires("LIB_C/1.0@%s/%s" % (self.user, self.channel))
        self.requires("LIB_B/1.0@%s/%s" % (self.user, self.channel))

    def build(self):
        self.output.warn("Building library...")
        time.sleep(2)
