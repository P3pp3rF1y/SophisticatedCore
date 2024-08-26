# MultiWorkspace
Setup for a multi-mod workspace

## Steps to run in intellij
- Clone this repo as usual
- Run following steps for submodules
```
git submodule init
git submodule update
```
- Open as project in IntelliJ using the build.gradle in the root folder
- Run getIntellijRuns in forgegradle runs
- Runs get created but they don't have module selected so when running it for the first time just select `.main` module of the project that's being run
- `workspace ...` runs run minecraft with all the mods in workspace
# Additional mods setup
Modify settings.gradle as well as workspace/build.gradle for all your mods.
Inside the mods/build.gradle you should have the following construct to make sure the mods are still compilable standalone as well as inside the multi-project:

    if (findProject(':McJtyLib') != null) {
        implementation project(':McJtyLib')
    } else {
        implementation fg.deobf (project.dependencies.create("com.github.mcjty:mcjtylib:${mcjtylib_version}") {
            transitive = false
        })
    }

To setup this project, just open the root (empty) build.gradle as a project in IDEA. Run genIntellijRuns and select the one for the 'workspace' module
