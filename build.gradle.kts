// Root project — plugins are applied by buildSrc convention plugins and module-level build files.
// No plugins applied here to avoid conflict with buildSrc classpath.

allprojects {
    group = property("ZZ143_GROUP") as String
    version = property("ZZ143_VERSION") as String
}
