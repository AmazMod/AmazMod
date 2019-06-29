package com.amazmod.service.util;

import android.os.Environment;

import com.amazmod.service.AmazModService;

import org.tinylog.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class ExecCommand {

    private Semaphore outputSem;
    private String output;
    private Semaphore errorSem;
    private String error;
    private Process p;
                                                //Runtime.getRuntime.run() modes that reads output and error streams
    public static final char MAKE_ARRAY = 'M';  //Execute the command separating each part in an Array
    public static final char NO_ARRAY = 'N';    //Execute the command directly as a single String
    public static final char NO_WAIT = 'O';     //Same as above but do not wait for result (adb hangs and cannot use the above) **it is the same as no mode as well**
    public static final char ADB = 'A';         //Special for adb, calls a background script that kills server at the end, exit value is from script **do not use quotes**

    public ExecCommand(char mode, String command) {

        switch (mode) {
            case MAKE_ARRAY:
                execMakeArray(command);
                break;
            case NO_ARRAY:
                execNoArray(command);
                break;
            case NO_WAIT:
                execNoWait(command);
                break;
            case ADB:
                execADB(command);
                break;
            default:
                Logger.error("ExecCommand mode not found!");
        }
    }

    public ExecCommand(String command) {
        execNoWait(command);
    }

    private void execADB(String command) {
        if(command.isEmpty()){
            Logger.error("execADB empty command!");
            return;
        }
        final String installScript = DeviceUtil.copyScriptFile(AmazModService.getContext(), "adb_script.sh").getAbsolutePath();
        final String adbCommand = String.format("log -pw -t'AmazMod ExecCommand' $(busybox nohup sh %s '%s' 2>&1 &)", installScript, command);
        Logger.trace("execADB adbCommand: " + adbCommand);

        try {
            p = Runtime.getRuntime().exec(new String[]{"sh", "-c", adbCommand},null, Environment.getExternalStorageDirectory());
            new OutputReader().start();
            new ErrorReader().start();
            //p.waitFor();
        } catch (Exception e) {
            Logger.debug(e, "execADB exception: {}", e.getMessage());
        }

        //Logger.debug("execADB finished p.exitValue: {}", p.exitValue());
        Logger.debug("execADB finished");

    }

    private void execMakeArray(String command) {
        Logger.trace("ExecCommand execMakeArray command: {}", command);
        try {
            p = Runtime.getRuntime().exec(makeArray(command));
            new OutputReader().start();
            new ErrorReader().start();
            p.waitFor();
        } catch (Exception e) {
            Logger.debug(e, "ExecCommand execMakeArray exception: {}", e.getMessage());
        }
        Logger.debug("ExecCommand execMakeArray finished p.exitValue: {}", p.exitValue());
    }

    private void execNoArray(String command) {
        Logger.trace("ExecCommand execNoArray command: {}", command);
        try {
            p = Runtime.getRuntime().exec(command);
            new OutputReader().start();
            new ErrorReader().start();
            p.waitFor();
        } catch (Exception e) {
            Logger.error(e, "ExecCommand execNoArray exception: {}", e.getMessage());
        }
        Logger.debug("ExecCommand execNoArray finished p.exitValue: {}", p.exitValue());
    }

    private void execNoWait(String command) {
        Logger.trace("ExecCommand execNoWait command: {}", command);
        try {
            p = Runtime.getRuntime().exec(command);
            new OutputReader().start();
            new ErrorReader().start();
            //p.waitFor();
        } catch (Exception e) {
            Logger.error(e, "ExecCommand execNoWait exception: {}", e.getMessage());
        }
        Logger.debug("ExecCommand execNoWait finished");
    }

    public String getOutput() {
        try {
            outputSem.acquire();
        } catch (InterruptedException e) {
            Logger.error(e, "ExecCommand getOutput exception: {}", e.getMessage());
        }
        String value = output;
        outputSem.release();
        return value;
    }

    public String getError() {
        try {
            errorSem.acquire();
        } catch (InterruptedException e) {
            Logger.error(e, "ExecCommand getError exception: {}", e.getMessage());
        }
        String value = error;
        errorSem.release();
        return value;
    }

    private class OutputReader extends Thread {
        OutputReader() {
            try {
                outputSem = new Semaphore(1);
                outputSem.acquire();
            } catch (InterruptedException e) {
                Logger.error(e, "ExecCommand OutputReader exception: {}", e.getMessage());
            }
        }

        public void run() {
            try {
                StringBuffer readBuffer = new StringBuffer();
                BufferedReader isr = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String buff;
                while ((buff = isr.readLine()) != null) {
                    readBuffer.append(buff);
                    //System.out.println(buff);
                    Logger.info("ExecCommand OutputReader.run buff: {}", buff);
                }
                output = readBuffer.toString();
                outputSem.release();
            } catch (IOException e) {
                Logger.error(e, "ExecCommand OutputError.run exception: {}", e.getMessage());
            }
        }
    }

    private class ErrorReader extends Thread {
        ErrorReader() {
            try {
                errorSem = new Semaphore(1);
                errorSem.acquire();
            } catch (InterruptedException e) {
                Logger.error(e, "ExecCommand ErrorReader exception: {}", e.getMessage());
            }
        }

        public void run() {
            try {
                StringBuffer readBuffer = new StringBuffer();
                BufferedReader isr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                String buff;
                while ((buff = isr.readLine()) != null) {
                    readBuffer.append(buff);
                }
                error = readBuffer.toString();
                errorSem.release();
            } catch (IOException e) {
                Logger.error(e, "ExecCommand ErrorReader.run exception: {}", e.getMessage());
            }
            if (error.length() > 0)
                Logger.error("ExecCommand ErrorReader.run error: {}", error);
                //System.out.println(error);
        }
    }

    private String[] makeArray(String command) {
        ArrayList<String> commandArray = new ArrayList<String>();
        StringBuilder buff = new StringBuilder();
        boolean lookForEnd = false;
        for (int i = 0; i < command.length(); i++) {
            if (lookForEnd) {
                if (command.charAt(i) == '\"') {
                    if (buff.length() > 0)
                        commandArray.add(buff.toString());
                    buff = new StringBuilder();
                    lookForEnd = false;
                } else {
                    buff.append(command.charAt(i));
                }
            } else {
                if (command.charAt(i) == '\"') {
                    lookForEnd = true;
                } else if (command.charAt(i) == ' ') {
                    if (buff.length() > 0)
                        commandArray.add(buff.toString());
                    buff = new StringBuilder();
                } else {
                    buff.append(command.charAt(i));
                }
            }
        }
        if (buff.length() > 0)
            commandArray.add(buff.toString());

        String[] array = new String[commandArray.size()];
        for (int i = 0; i < commandArray.size(); i++) {
            array[i] = commandArray.get(i);
            Logger.info("ExecCommand makeArray array[{}]: {}", i, array[i]);
        }

        return array;
    }
}