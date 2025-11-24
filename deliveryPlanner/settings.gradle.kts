pluginManagement {
    repositories {
        // 1. 阿里云公共仓库 (最快)
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // 2. 阿里云 Google 镜像
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // 3. 阿里云 Gradle 插件镜像
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 同样配置阿里云
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        google()
        mavenCentral()
        // 高德地图的仓库 (如果有用到)
        maven { url = uri("https://repo1.maven.org/maven2") }
    }
}

rootProject.name = "deliveryPlanner"
include(":app")
 