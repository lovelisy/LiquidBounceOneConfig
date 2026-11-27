plugins {
    `kotlin-dsl`
}

group = "net.ccbluex"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(25)
}

gradlePlugin {
    plugins {
        create("buildLogic") {
            id = "net.ccbluex.liquidbounce.build-logic"
            implementationClass = "net.ccbluex.LiquidBounceBuildLogicPlugin"
        }
    }
}
