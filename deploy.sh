#!/bin/bash
appPort=9005
appName=idea-activation-code
appDir=/mnt/workspace/project/${appName}
appTargetDir=${appDir}/target/
appLogDir=${appDir}/log/logback_info.log
jvmOption=
springOption=

# 颜色输出
printLog() {
    #字颜色变量
    #黑色
    BLACK="\033[1;2;30m"
    #红色
    RED="\033[1;2;31m"
    #绿色
    GREEN="\033[1;2;32m"
    #黄色
    YELLOW="\033[1;2;33m"
    #蓝色
    BLUE="\033[1;2;34m"
    #紫色
    PURPLE="\033[1;2;35m"
    #天绿色
    SKY_GREEN="\033[1;2;36m"
    #白色
    WHITE="\033[1;2;37m"
    #默认
    DEFAULT="\033[0m"

    function printByColor() {
        echo -e "$DEFAULT[$1DEPLOY-$2$DEFAULT] $1""$3""$DEFAULT"
    }

    if [[ ! -n $1 ]]; then
        echo
    elif [[ $1 == info ]]; then
       printByColor ${BLUE} INFO "$2"
    elif [[ $1 == error ]]; then
        printByColor ${RED} ERROR "$2"
    elif [[ $1 == success ]]; then
        printByColor ${GREEN} SUCCESS "$2"
    elif [[ $1 == warning ]]; then
        printByColor ${YELLOW} WARNING "$2"
    fi
}

info() {
    printLog info "$1"
}

error() {
    printLog error "$1"
}

success() {
    printLog success "$1"
}

warning() {
    printLog warning "$1"
}

# 执行shell成功，否则退出执行
function execResult() {
    if [[ $? -ne 0 ]]; then
        error "$1 失败"
        exit
    else
        success "$1 成功"
    fi
}
function execResultOnlyFailed() {
    if [[ $? -ne 0 ]]; then
        error "$1失败"
        exit
    fi
}

# 帮助信息
echoHelp() {
    warning "参数错误"
    info "-f        前台方式启动"
    info "-b        后台方式启动"
    info "-cf       编译然后以前台方式启动"
    info "-cb       编译然后以后台方式启动"
    info "-pcf      拉取、编译然后以前台方式启动"
    info "-pcb      拉取、编译然后以后台方式启动"
    info "-p        拉取新代码"
    info "-c        编译代码"
    info "-l        查看项目日志"
    info "-s        项目端口是否被使用"
    info "-k        kill使用当前项目端口的程序"
}

# 查看项目日志
viewLog() {
    info "查看项目日志"
    if [[ ${appLogDir} ]]; then
        tailf ${appLogDir}
        execResult "查看项目日志"
    else
        warning "未指定日志路径"
    fi
}

# 根据端口号查询对应应用的PID
checkPortState(){
    info "查询端口: "${appPort}
    appPid=$(netstat -lnp|grep ${appPort} | sed -e 's/^.*LISTEN\s*//' -e 's/\/java//')

    if [[ ${appPid} ]]; then
        info ${appPort}"端口被占用，占用程序PID："${appPid}
    else
        info $1"端口未被占用"
    fi
    echo
}

# 更新项目
updateApp() {
    cd ${appDir}
    execResultOnlyFailed "进入项目目录"

    info "开始拉取更新"
    git pull origin master
    execResult "拉取更新"
    echo
}

# 编译项目
compileApp() {
    cd ${appDir}
    execResultOnlyFailed "进入项目目录"

    info "开始编译"
    mvn clean package -Dmaven.test.skip=true
    execResult "编译"
    echo
}

# 结束项目
killApp() {
    info "查询端口: "${appPort}
    appPid=$(netstat -lnp|grep ${appPort} | sed -e 's/^.*LISTEN\s*//' -e 's/\/java//')

    if [[ ${appPid} ]]; then
        info ${appPort}"端口被占用，占用程序PID："${appPid}
        info "kill占用程序"
        kill -9 ${appPid}
        execResult "关闭当前应用"
    else
        info $1"端口未被占用"
    fi
    echo
    checkPortState
}

# 运行项目
runApp() {

    killApp

    cd ${appTargetDir}
    execResultOnlyFailed "进入编译目录： $appTargetDir"
    if [[ -n $1 && $1 == "-f" ]]; then
        info "以前台方式启动"
        java ${jvmOption} -jar *.jar ${springOption}
        execResultOnlyFailed "以前台方式启动"
    elif [[ -n $1 && $1 == "-b" ]]; then
        info "以后台方式启动"
        nohup java ${jvmOption} -jar *.jar ${springOption} > /dev/null 2>&1 &
        execResultOnlyFailed "以后台方式启动"
        echo
        viewLog
    fi
}

parseParam() {
    case $1 in
    -f)
        info "前台启动"
        info ""
        runApp -f
        ;;
    -b)
        info "后台启动"
        info ""
        runApp -b
        ;;
    -cf)
        info "编译然后以前台方式启动"
        info ""
        compileApp
        runApp -f
        ;;
    -cb)
        info "编译然后以后台方式启动"
        info ""
        compileApp
        runApp -b
        ;;
    -pcf)
        info "拉取、编译然后以前台方式启动项目"
        info ""

        updateApp
        compileApp
        runApp -f
        ;;
    -pcb)
        info "拉取、编译然后以后台方式启动项目"
        info ""

        updateApp
        compileApp
        runApp -b
        ;;
    -c)
        info "编译项目"
        info ""

        compileApp
        ;;
    -p)
        info "更新项目"
        info ""

        updateApp
        ;;
    -l)
        info "查看日志"
        info ""

        viewLog
        ;;
    -s)
        info "查看项目使用端口占用情况"
        info ""

        checkPortState
        ;;
    -k)
        info "直接关闭使用项目端口的应用"
        info ""

        killApp
        ;;
    *)
        echoHelp;;
    esac
    exit
}

if [[  $# == 1 ]] ; then
    parseParam $1
else
    echoHelp
fi
echo

