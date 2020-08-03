import hudson.slaves.OfflineCause.ByCLI
import hudson.model.Computer.ListPossibleNames


def agents = params.startsWith
pipeline {
    agent { label 'master' }

    stages {
        stage('Finding Nodes By Label') {
            steps {
                echo 'Hello World'

                nodeNames("${agents}")
            }
        }
    }
}

@NonCPS
def nodeNames(label) {
    def nodeList = []
    jenkins.model.Jenkins.instance.computers.each { c ->
        if (c.node.labelString.contains(label)) {
            nodeList.add(c.node.selfLabel.name)
        }
    }
    println nodeList
    //return nodes
    if (nodeList.size() >= 1) {
        for (n in nodeList) {
            if (Jenkins.instance.getNode(n).toComputer().isOnline()) {
                echo "$n agent is online and setting accepting task to false / closing the queue"
                def plannedRebootCause = new ByCLI("Planned Windows node reboot from ${BUILD_URL}")
                Jenkins.instance.getNode(n).toComputer().setAcceptingTasks(false)
                def bool = waitlaw(n)
                if (bool) {
                    // execute ANY command
                    def listener = Jenkins.get()
                                    .getItemByFullName(env.JOB_NAME)
                                    .getBuildByNumber(Integer.parseInt(env.BUILD_NUMBER))
                                    .getListener()

                    def launcher = Jenkins.instance.getNode(n).createLauncher(listener)

                    // https://javadoc.jenkins-ci.org/hudson/Launcher.ProcStarter.html
                    def proc = launcher.launch().cmdAsSingleString('shutdown /r /f /t 00').start()
                    proc.join() // wait for completion
                    //println 'STDOUT: ' + proc.getStdout().getText()
                    //println 'STDERR: ' + proc.getStderr().getText()
                    // echo "$n node wait until online" //some spam for test
                    Jenkins.instance.getNode(n).getComputer().waitUntilOnline()
                    echo "$n agent setting accepting task to true / opening the queue"
                    Jenkins.instance.getNode(n).getComputer().setAcceptingTasks(true)
                } else {
                    echo "Completing the job on node $n took much time"
                }
            } else {
                echo "$n node is offline. You need to bring it online first"
            }
        }
    } else {
        echo "you dont have any agents with the name $label"
    }
}

@NonCPS
def waitlaw(String str) {
    def a = str
    if (Jenkins.instance.getNode(a).toComputer().countBusy() == 0) {
        echo 'Returning true'
        return true
} else {
        while (Jenkins.instance.getNode(a).toComputer().countBusy() != 0) {
            echo 'Running until the number the number of jobs is equal to zero'
            if (Jenkins.instance.getNode(a).toComputer().countBusy() == 0) {
                echo ' Returns true once the number of jobs is zero.'
                return true
            }
        }
    }
}
