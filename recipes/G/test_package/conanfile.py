from conans import ConanFile
import os


channel = os.getenv("CONAN_CHANNEL", "stable")
username = os.getenv("CONAN_USERNAME", "lasote")


class BTestConan(ConanFile):
    settings = "os", "compiler", "build_type", "arch"
    requires = "LIB_G/1.0@%s/%s" % (username, channel)

    def test(self):
        self.output.info("Test OK!")