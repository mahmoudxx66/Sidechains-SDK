**STF**
---------

STF is test framework to run integration tests.

It is possible to test a SC node or nodes with or without real MC node connection.

**Requirements**

1. Install Python 3 anf PIP
```
sudo apt install python
sudo apt-get -y install python3-pip
```
2. Install [JetBrains PyCharm Community](https://www.jetbrains.com/pycharm/download) and open qa subproject in it.
3. Checkout [ZEN project](https://github.com/HorizenOfficial/zen) and execute commands from its README.MD file.


**Additional settings**

1. Example for Linux:
```
sudo nano /etc/environment
```
2. In this file after Path from the new line put Environment variables:
```
BITCOINCLI=/home/yourName/yourProjectDirectory/zen/zen-cli
BITCOIND=/home/yourName/yourProjectDirectory/zen/zend
SIDECHAIN_SDK=/home/yourName/yourProjectDirectory/Sidechains-SDK
```
change yourName and yourProjectDirectory to the relevant one.
4. Save file, exit and restart your computer.
5. Then make sure that environment variables are set:
```
echo $BITCOINCLI
echo $BITCOIND
echo $SIDECHAIN_SDK
```
verify that all path are valid and are referenced to existing files.


**Execution**
1. Install Maven, go to root folder of Sidechain-SDK and run Maven to clean previous and build a new JAR
```
   cd ..
   mvn clean package -Dmaven.test.skip=true
```
2. You can run all tests using command.
```
cd qa
python run_sc_tests.py
```
Or run individual test using command
```
cd qa
python <test.py>
```
replacing <test.py> with the name of test that you want to execute.


**Template configuration files**

Template configuration files located in resources directory. 

File template.conf is the template for testing SC node(s) connected to MC node(s).

File template_predefined_genesis.conf is the template for testing SC node(s) standalone mode (without any connections to MC node(s)).

**Debugging**

In order to run a python test for debugging SDK application, the following procedure can be applied:

1) When starting a sc node in the py test, add the option '_-agentlib_' to the _extra_args_ list in the relevant API call, for example:
   ```
   start_sc_nodes(1, self.options.tmpdir, extra_args=['-agentlib'])
   ```
    This will cause the simpleApp process to start with the debug agent acting as a server. The process will wait until the debugger has been connected.


2) Run the py test.

   If needed, in order to increase the rest API timeout, use the optional argument _--restapitimeout=<timeout_value_in_secs>_, for example:
   ```
   python sc_forward_transfer.py --restapitimeout=200
   ```
   
3) Attach the debugger to the simpleApp process.

   For instance, if using IntelliJ:


- Press `Ctrl+Alt+F5` or choose **_Run | Attach to Process_** from the main menu.
- Select the process to attach to from the list of the running local processes. The processes launched with the debug agent are shown under _**Java**_. Those that don't use a debug agent are listed under **_Java Read Only_**.