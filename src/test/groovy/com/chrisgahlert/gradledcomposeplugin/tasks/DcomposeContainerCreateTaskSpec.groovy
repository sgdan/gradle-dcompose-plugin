package com.chrisgahlert.gradledcomposeplugin.tasks

import com.chrisgahlert.gradledcomposeplugin.AbstractDcomposeSpec

/**
 * Created by chris on 17.04.16.
 */
class DcomposeContainerCreateTaskSpec extends AbstractDcomposeSpec {

    def 'create should work successfully'() {
        given:
        buildFile << """
            $DEFAULT_BUILD_FILE

            ${copyTaskConfig('main', '/etc/group')}
        """

        when:
        def result = runTasksSuccessfully 'copy'

        then:
        result.wasExecuted(':createMainContainer')
        file('build/copy/group').text.startsWith('root:x:0:')
    }

    def 'create should be up-to-date'() {
        given:
        buildFile << DEFAULT_BUILD_FILE
        runTasksSuccessfully 'createMainContainer'

        when:
        def result = runTasksSuccessfully 'createMainContainer'

        then:
        result.wasUpToDate(':createMainContainer')
    }

    def 'create should not be up-to-date when inputs change'() {
        given:
        buildFile << DEFAULT_BUILD_FILE

        runTasksSuccessfully 'createMainContainer'
        buildFile << "dcompose.main.command = ['echo', 'yeehaw']"

        when:
        def result = runTasksSuccessfully 'createMainContainer'

        then:
        !result.wasUpToDate(':createMainContainer')
        result.wasExecuted(':createMainContainer')
    }

    def 'create should not be up-to-date when underlying image changed'() {
        given:
        def dockerFile = file('docker/Dockerfile')
        dockerFile.text = 'FROM busybox:1.24.2-musl'

        buildFile << """
            dcompose {
                main {
                    baseDir = file('docker/')
                    command = ['echo', 'test']
                    tag = 'createtestimage'
                }
            }
        """

        runTasksSuccessfully 'createMainContainer'
        dockerFile << '\nCMD ["/bin/sleep", "300"]'


        when:
        def result = runTasksSuccessfully 'createMainContainer'

        then:
        !result.wasUpToDate(':createMainContainer')
        result.wasExecuted(':createMainContainer')
    }

    def 'create should not preserve volumes by default'() {
        given:
        buildFile << """
            dcompose {
                preserve {
                    image = '$DEFAULT_IMAGE'
                    command = ['sh', '-c', 'echo ptest > /test/content']
                    waitForCommand = true
                    volumes = ['/test']
                }
            }

            ${copyTaskConfig('preserve', '/test')}
        """

        runTasksSuccessfully 'startPreserveContainer', 'removePreserveContainer'

        when:
        runTasksSuccessfully 'copy'

        then:
        file('build/copy/content').text.isEmpty()
    }

    def 'create should preserve volumes after remove when enabled'() {
        given:
        buildFile << """
            dcompose {
                preserve {
                    image = '$DEFAULT_IMAGE'
                    command = ['sh', '-c', 'echo ptest > /test/content']
                    waitForCommand = true
                    volumes = ['/test']
                    preserveVolumes = true
                }
            }

            ${copyTaskConfig('preserve', '/test')}
        """

        runTasksSuccessfully 'startPreserveContainer', 'removePreserveContainer'

        when:
        runTasksSuccessfully 'copy'

        then:
        file('build/copy/content').text.trim() == 'ptest'
    }

    def 'create should not preserve image-provided volumes by default'() {
        given:
        file('docker/Dockerfile').text = """
            FROM $DEFAULT_IMAGE
            VOLUME /myvol
        """.stripIndent()

        buildFile << """
            dcompose {
                preserve {
                    baseDir = file('docker')
                    command = ['sh', '-c', 'echo ptest > /myvol/content']
                    waitForCommand = true
                }
            }

            ${copyTaskConfig('preserve', '/myvol')}
        """

        runTasksSuccessfully 'startPreserveContainer', 'removePreserveContainer'

        when:
        runTasksSuccessfully 'copy'

        then:
        file('build/copy/content').text.isEmpty()
    }

    def 'create should preserve image-provided volumes after remove when enabled'() {
        given:
        file('docker/Dockerfile').text = """
            FROM $DEFAULT_IMAGE
            VOLUME /myvol
        """.stripIndent()

        buildFile << """
            dcompose {
                preserve {
                    baseDir = file('docker')
                    command = ['sh', '-c', 'echo ptest > /myvol/content']
                    waitForCommand = true
                    preserveVolumes = true
                }
            }

            ${copyTaskConfig('preserve', '/myvol')}
        """

        runTasksSuccessfully 'startPreserveContainer', 'removePreserveContainer'

        when:
        runTasksSuccessfully 'copy'

        then:
        file('build/copy/content').text.trim() == 'ptest'
    }

    def 'create should not preserve volumes on update by default'() {
        given:
        buildFile << """
            dcompose {
                preserve {
                    image = '$DEFAULT_IMAGE'
                    command = ['sh', '-c', 'echo ptest > /test/content']
                    waitForCommand = true
                    volumes = ['/test']
                }
            }

            ${copyTaskConfig('preserve', '/test')}
        """

        runTasksSuccessfully 'startPreserveContainer'
        buildFile << "dcompose.preserve.volumes << '/test2'"

        when:
        def result = runTasksSuccessfully 'copy'

        then:
        !result.wasUpToDate(':createPreserveContainer')
        file('build/copy/content').text.isEmpty()
    }

    def 'create should preserve volumes on update when enabled'() {
        given:
        buildFile << """
            dcompose {
                preserve {
                    image = '$DEFAULT_IMAGE'
                    command = ['sh', '-c', 'echo ptest > /test/content']
                    waitForCommand = true
                    volumes = ['/test']
                    preserveVolumes = true
                }
            }

            ${copyTaskConfig('preserve', '/test')}
        """

        runTasksSuccessfully 'startPreserveContainer'
        buildFile << "dcompose.preserve.volumes << '/test2'"

        when:
        def result = runTasksSuccessfully 'copy'

        then:
        !result.wasUpToDate(':createPreserveContainer')
        file('build/copy/content').text.trim() == 'ptest'
    }

}
