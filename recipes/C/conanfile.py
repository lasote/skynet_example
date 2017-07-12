import time
from conans import ConanFile

class CConan(ConanFile):
    name = "LIB_C"
    version = "1.0"
    license = "MIT"
    url = "https://github.com/lasote/skynet_example"
    settings = "os", "compiler", "build_type", "arch"

    def build(self):
        self.output.warn("Building library...")
        time.sleep(2)
