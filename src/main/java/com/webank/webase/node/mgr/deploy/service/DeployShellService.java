/**
 * Copyright 2014-2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.webank.webase.node.mgr.deploy.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.fisco.bcos.web3j.crypto.EncryptType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ReactiveTypeDescriptor;
import org.springframework.stereotype.Component;

import com.webank.webase.node.mgr.base.code.ConstantCode;
import com.webank.webase.node.mgr.base.enums.ScpTypeEnum;
import com.webank.webase.node.mgr.base.exception.NodeMgrException;
import com.webank.webase.node.mgr.base.properties.ConstantProperties;
import com.webank.webase.node.mgr.base.tools.IPUtil;
import com.webank.webase.node.mgr.base.tools.JsonTools;
import com.webank.webase.node.mgr.base.tools.SshTools;
import com.webank.webase.node.mgr.base.tools.cmd.ExecuteResult;
import com.webank.webase.node.mgr.base.tools.cmd.JavaCommandExecutor;

import lombok.extern.log4j.Log4j2;

/**
 * Java call shell script and system command.
 */
@Log4j2
@Component
public class DeployShellService {

    @Autowired
    private ConstantProperties constant;
    @Autowired
    private PathService pathService;
    private static final String OPERATE_FUNCTION_CHECK = "check";
    private static final String OPERATE_FUNCTION_DOCKER = "docker";
    private static final String OPERATE_FUNCTION_INIT = "init";

    /**
     * @param typeEnum
     * @param sshUser
     * @param ip
     * @param sshPort
     * @param src
     * @param dst
     * @return
     */
    public void scp(ScpTypeEnum typeEnum, String sshUser, String ip, int sshPort, String src, String dst) {
        if (typeEnum == ScpTypeEnum.UP) {
            // scp files to remote
            if (Files.isRegularFile(Paths.get(src))) {
                // if src is file, create parent directory of dst on remote
                String parentOnRemote = Paths.get(dst).getParent().toAbsolutePath().toString();
                SshTools.createDirOnRemote(ip, parentOnRemote,sshUser,sshPort,constant.getPrivateKey());
            }
            if (Files.isDirectory(Paths.get(src))) {
                // if src is directory, create dst on remote
                SshTools.createDirOnRemote(ip, dst,sshUser,sshPort,constant.getPrivateKey());
            }
        }

        String command = String.format("bash -x -e %s -t %s -i %s -u %s -p %s -s '%s' -d '%s' %s",
                constant.getScpShell(), typeEnum.getValue(), ip, sshUser, sshPort, src, dst,
                IPUtil.isLocal(dst) ? " -l " : "");
        log.info("exec file send command: [{}]", command);
        ExecuteResult result = JavaCommandExecutor.executeCommand(command, constant.getExecHostInitTimeout());

        if (result.failed()) {
            log.error("Send files from [{}] to [{}:{}] failed.", src, ip, dst);
            throw new NodeMgrException(ConstantCode.TRANSFER_FILES_ERROR.attach(result.getExecuteOut()));
        }
    }


    /**
     * @param ip        Required.
     * @param port      Default 22.
     * @param user      Default root.
     * @param chainRoot chain root on host, default is /opt/fisco/{chain_name}.
     * @return
     */
    public void execHostOperateToInit(String ip, int port, String user, String chainRoot) {
        ExecuteResult result = this.execHostOperate(ip, port, user, "", chainRoot, OPERATE_FUNCTION_INIT, 0);
        if (result.failed()) {
            throw new NodeMgrException(ConstantCode.EXEC_HOST_INIT_SCRIPT_ERROR.attach(result.getExecuteOut()));
        }
    }

    /**
     * @param ip        Required.
     * @param port      Default 22.
     * @param user      Default root.
     * @param pwd       Not required.
     * @param chainRoot chain root on host, default is /opt/fisco/{chain_name}.
     * @param function command for host_operate.sh, init or check, docker
     * @return
     */
    public ExecuteResult execHostOperate(String ip, int port, String user, String pwd, String chainRoot,
        String function, int nodeCount) {
        log.info("Exec execHostOperate method for [{}@{}:{}#{}],function:{},nodeCount:{}",
            user, ip, port, pwd, function, nodeCount);
        // init or check: default init
        int newport = port <= 0 || port > 65535 ? SshTools.DEFAULT_SSH_PORT : port;
        String newUser = StringUtils.isBlank(user) ? SshTools.DEFAULT_SSH_USER : user;
        String useDockerCommand = constant.isUseDockerSDK() ? "" : "-c";
        String passwordParam = StringUtils.isBlank(pwd) ? "" : String.format(" -p %s ", pwd);
        String chainRootParam = StringUtils.isBlank(chainRoot) ? "" : String.format(" -n %s ",chainRoot);
        String nodeCountParam = nodeCount == 0 ? "" : String.format(" -C %d ", nodeCount);


        String command = String.format("bash -x -e %s %s -H %s -P %s -u %s %s %s %s %s ",
                constant.getNodeOperateShell(), function, ip, newport, newUser, passwordParam,
            chainRootParam, nodeCountParam, useDockerCommand);

        return JavaCommandExecutor.executeCommand(command, constant.getExecHostInitTimeout());
    }

    /**
     * build_chain.sh
     * todo 指定多个ip port进行生成
     * @param encryptType
     * @param ipLines
     * @return
     */
    public void execBuildChain(byte encryptType,
                                        String[] ipLines,
                                        String chainName) {
        Path ipConf = pathService.getIpConfig(chainName);
        log.info("Exec execBuildChain method for [{}], chainName:[{}], ipConfig:[{}]",
                JsonTools.toJSONString(ipLines), chainName, ipConf.toString());
        try {
            if ( ! Files.exists(ipConf.getParent())) {
                Files.createDirectories(ipConf.getParent());
            }
            Files.write(ipConf, Arrays.asList(ipLines));
        } catch (IOException e) {
            log.error("Write ip conf file:[{}] error", ipConf.toAbsolutePath().toString(), e);
            throw new NodeMgrException(ConstantCode.SAVE_IP_CONFIG_FILE_ERROR);
        }

        // ports start
        String shellPortParam = String.format(" -p %s,%s,%s",
                constant.getDefaultP2pPort(), constant.getDefaultChannelPort(),constant.getDefaultJsonrpcPort());

        // build_chain.sh only support docker on linux
        // command e.g : build_chain.sh -f ipconf -o outputDir [ -p ports_start ] [ -g ] [ -d ] [ -e exec_binary ]
        String command = String.format("bash -e %s -S -f %s -o %s %s %s %s %s",
                // build_chain.sh shell script
                constant.getBuildChainShell(),
                // ipconf file path
                ipConf.toString(),
                // output path
                pathService.getChainRootString(chainName),
                // port param
                shellPortParam,
                // guomi or standard
                encryptType == EncryptType.SM2_TYPE ? "-g " : "",
                // only linux supports docker model
                SystemUtils.IS_OS_LINUX ? " -d " : "",
                // use binary local
                StringUtils.isBlank(constant.getFiscoBcosBinary()) ? "" :
                        String.format(" -e %s ", constant.getFiscoBcosBinary())
        );

        ExecuteResult result = JavaCommandExecutor.executeCommand(command, constant.getExecBuildChainTimeout());

        if (result.failed()) {
            throw new NodeMgrException(ConstantCode.EXEC_BUILD_CHAIN_ERROR.attach(result.getExecuteOut()));
        }
    }

    /**
     *
     * @param encryptType
     * @param chainName
     * @param newAgencyName
     * @return
     */
    public ExecuteResult execGenAgency(byte encryptType,
                                       String chainName,
                                       String newAgencyName) {
        log.info("Exec execGenAgency method for chainName:[{}], newAgencyName:[{}:{}]", chainName, newAgencyName, encryptType);

        Path certRoot = this.pathService.getCertRoot(chainName);

        if (Files.notExists(certRoot)) {
            // file not exists
            log.error("Chain cert : [{}] not exists in directory:[{}] ", chainName, Paths.get(".").toAbsolutePath().toString());
            throw new NodeMgrException(ConstantCode.CHAIN_CERT_NOT_EXISTS_ERROR);
        }

        // build_chain.sh only support docker on linux
        String command = String.format("bash -x -e %s -c %s -a %s %s",
                // gen_agency_cert.sh shell script
                constant.getGenAgencyShell(),
                // chain cert dir
                certRoot.toAbsolutePath().toString(),
                // new agency name
                newAgencyName,
                encryptType == EncryptType.SM2_TYPE ?
                        String.format(" -g %s", pathService.getGmCertRoot(chainName).toAbsolutePath().toString())
                        : ""
        );

        return JavaCommandExecutor.executeCommand(command, constant.getExecShellTimeout());
    }


    /**
     *
     * @param encryptType
     * @param chainName
     * @param agencyName
     * @param newNodeRoot
     * @return
     */
    public ExecuteResult execGenNode(byte encryptType,
                                     String chainName,
                                     String agencyName,
                                     String newNodeRoot) {
        log.info("Exec execGenNode method for chainName:[{}], node:[{}:{}:{}]",
                chainName, encryptType, agencyName, newNodeRoot);

        Path agencyRoot = this.pathService.getAgencyRoot(chainName,agencyName);

        // build_chain.sh only support docker on linux
        String command = String.format("bash -x -e %s -c %s -o %s %s",
                // gen_node_cert.sh shell script
                constant.getGenNodeShell(),
                // agency cert root
                agencyRoot.toAbsolutePath().toString(),
                // new node dir
                newNodeRoot,
                encryptType == EncryptType.SM2_TYPE ?
                        String.format(" -g %s", pathService.getGmAgencyRoot(chainName,agencyName).toAbsolutePath().toString()) : ""
        );

        return JavaCommandExecutor.executeCommand(command, constant.getExecShellTimeout());
    }

    /**
     * check host memory/cpu/port
     * TODO check node port, front port;
     * todo if ssh fail, ip port user error
     * @param ip        Required.
     * @param port      Default 22.
     * @param user      Default root.
     * @param nodeCount      Node count in single host
     * @return
     */
    public void execHostCheck(String ip, int port, String user, int nodeCount) {
        log.info("Exec execHostCheck method for [{}@{}:{}]", user, ip, port);

        ExecuteResult result = this.execHostOperate(ip, port, user, "", "", OPERATE_FUNCTION_CHECK, nodeCount);
        if (result.failed()) {
            if (result.getExitCode() == 3) {
                throw new NodeMgrException(ConstantCode.EXEC_HOST_CHECK_SCRIPT_ERROR_FOR_MEM.attach(result.getExecuteOut()));
            }
            if (result.getExitCode() == 4) {
                throw new NodeMgrException(ConstantCode.EXEC_HOST_CHECK_SCRIPT_ERROR_FOR_CPU.attach(result.getExecuteOut()));
            }
        }
    }

    /**
     * check host docker/docker-compose hello_world
     * @param ip        Required.
     * @param port      Default 22.
     * @param user      Default root.
     * @return
     */
    public void execDockerCheck(String ip, int port, String user) {
        log.info("Exec execHostCheck method for [{}@{}:{}]", user, ip, port);

        ExecuteResult result = this.execHostOperate(ip, port, user, "", "", OPERATE_FUNCTION_DOCKER, 0);
        if (result.failed()) {
            throw new NodeMgrException(ConstantCode.EXEC_DOCKER_CHECK_SCRIPT_ERROR.attach(result.getExecuteOut()));
        }
    }
}