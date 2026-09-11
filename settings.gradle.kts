rootProject.name = "orchestreeai"
include("orchestreeai-backend-server")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("orchestreeai-backend-server/gradle/libs.versions.toml"))
        }
    }
}
