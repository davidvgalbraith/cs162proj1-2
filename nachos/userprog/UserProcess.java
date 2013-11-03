package nachos.userprog;

import java.io.EOFException;

import java.util.HashMap;

import nachos.machine.Coff;
import nachos.machine.CoffSection;
import nachos.machine.Kernel;
import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.OpenFile;
import nachos.machine.Processor;
import nachos.machine.TranslationEntry;
import nachos.threads.Condition;
import nachos.threads.KThread;
import nachos.threads.Lock;
import nachos.threads.ThreadedKernel;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 * 
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 * 
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {

	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
		// part1: When any process is started, its file descriptors(0,1) must
		// refer to std in/out.
		fileDescriptors.put(0, UserKernel.console.openForReading()); // stdin
		fileDescriptors.put(1, UserKernel.console.openForWriting()); // stdout
		staticLock.acquire(); // set processID, increment nextProcessID and
								// numProcesses
		processID = nextProcessID;
		nextProcessID++;
		numProcesses++;
		staticLock.release();
// 	test case for checking validity of fileDescriptors inputs
//      	int x = 0;
//		while (x < 15){
//          		System.out.println(x);
//      	        fileDescriptors.put(x,null);
//      	        x++;
//          }
	}

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 * 
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
		return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 * 
	 * @param name
	 *            the name of the file containing the executable.
	 * @param args
	 *            the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args))
			return false;
		new UThread(this).setName(name).fork();

		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 * 
	 * @param vaddr
	 *            the starting virtual address of the null-terminated string.
	 * @param maxLength
	 *            the maximum number of characters in the string, not including
	 *            the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 *         found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to read.
	 * @param data
	 *            the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to read.
	 * @param data
	 *            the array where the data will be stored.
	 * @param offset
	 *            the first byte to write in the array.
	 * @param length
	 *            the number of bytes to transfer from virtual memory to the
	 *            array.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= pageTable.length * Machine.processor().pageSize)
			return 0;
		int pageno = Machine.processor().pageFromAddress(vaddr);
		int offfset = Machine.processor().offsetFromAddress(vaddr);
		TranslationEntry entry = pageTable[pageno];
		int total = 0;
		while (length > 0 && entry.valid) {
			int amount = Math.min(Machine.processor().pageSize, length);
			System.arraycopy(memory, entry.ppn * Machine.processor().pageSize
					+ offfset, data, offset, amount);
			entry.used = true;
			length -= Machine.processor().pageSize;
			
			entry = pageTable[pageno];
			pageno += 1;
			total += amount;
		}
		return total;
	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to write.
	 * @param data
	 *            the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to write.
	 * @param data
	 *            the array containing the data to transfer.
	 * @param offset
	 *            the first byte to transfer from the array.
	 * @param length
	 *            the number of bytes to transfer from the array to virtual
	 *            memory.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);
		byte[] memory = Machine.processor().getMemory();
		// for now, don't assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= pageTable.length * Machine.processor().pageSize)
			return 0;
		int pageno = Machine.processor().pageFromAddress(vaddr);
		int offfset = Machine.processor().offsetFromAddress(vaddr);
		TranslationEntry entry = pageTable[pageno];
		int total = 0;
		while (length > 0 && entry.valid && !entry.readOnly) {
			int amount = Math.min(Machine.processor().pageSize, length);
			System.arraycopy(data, offset, memory,
					entry.ppn * Machine.processor().pageSize + offfset, amount);
			entry.dirty = true;
			entry.used = true;
			length -= Machine.processor().pageSize;
			
			entry = pageTable[pageno];
			pageno += 1;
			total += amount;
		}
		return total;
	}

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 * 
	 * @param name
	 *            the name of the file containing the executable.
	 * @param args
	 *            the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		} catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}
		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
				return false;
			}
			numPages += section.getLength();
		}

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages * pageSize;

		// and finally reserve 1 page for arguments
		numPages++;

		if (!loadSections())
			return false;

		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;
		for (int i = 0; i < argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
			stringOffset += 1;
		}

		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 * 
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		if (numPages > Machine.processor().getNumPhysPages()) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}
		pageTable = new TranslationEntry[numPages];
		int index = 0;
		// load sections
		UserKernel.memoryLock.acquire();
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;
				int ppn = UserKernel.getMemory().pop();
				TranslationEntry entry = new TranslationEntry(vpn, ppn, true,
						section.isReadOnly(), true, false);
				pageTable[index] = entry;
				index += 1;
				section.loadPage(i, ppn);
			}
		}
		for (int k = index; k < pageTable.length; k++) {
			TranslationEntry e = new TranslationEntry(k,
					UserKernel.memory.pop(), true, false, false, false);

			pageTable[k] = e;
		}
		UserKernel.memoryLock.release();
		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		UserKernel.memoryLock.acquire();
		for (TranslationEntry e : pageTable) {
			UserKernel.memory.add(e.ppn);
		}
		UserKernel.memoryLock.release();
	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call.
	 */
	private int handleHalt() {
		if (processID == 0) { // part1: so, the halt() call can only be invoked
								// by the "root" process
			Machine.halt();
		} else {
			return -1; // part1: When a system call indicates an error condition
						// to the user, it should return -1
		}
		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}

	private int handleCreate(int virtualAddress) { // part1: syscall create()
		String filename = readVirtualMemoryString(virtualAddress, 256);
		if (filename != null && !filename.isEmpty()) {
			OpenFile givenFile = ThreadedKernel.fileSystem.open(filename, true);
			if (givenFile != null) {
				count = count + 1;
				fileDescriptors.put(count, givenFile);   ///fixed bug
				return count;
			}
		}	
		return -1;
	}

	private int handleOpen(int virtualAddress) { // part1: syscall open()
		String filename = readVirtualMemoryString(virtualAddress, 256);
		if (filename != null && !filename.isEmpty()) {
			OpenFile givenFile = ThreadedKernel.fileSystem.open(filename, false);
			if (givenFile != null) {
				count = count + 1;
				fileDescriptors.put(count, givenFile);   ///fixed bug
				return count;
			}
		}	
		return -1;
	}

	private int handleRead(int fileNum, int bufferAddress, int length) { // part1:																// syscall
		if (length < 0) return -1;
		if (fileDescriptors.get(fileNum) != null) {
			byte[] dataToWrite = new byte[length];
			int readAmount = fileDescriptors.get(fileNum).read(dataToWrite, 0,
					length);
			if (readAmount == -1) {
				return -1;
			}
			return this.writeVirtualMemory(bufferAddress, dataToWrite, 0, readAmount);  //fixed a bug
		}
		return -1;
	}

	private int handleWrite(int fileNum, int bufferAddress, int length) { // part1:
		if (length < 0) return -1;
		if (fileDescriptors.get(fileNum) != null) {
			byte[] dataToWrite = new byte[length];
			int readAmount = readVirtualMemory(bufferAddress, dataToWrite);
			int writeAmount = fileDescriptors.get(fileNum).write(dataToWrite,
					0, readAmount);
			return writeAmount;
		}
		return -1;
	}
	
	private int handleClose(int fileNum) { // part1: syscall close()
		if (fileDescriptors.get(fileNum) != null) {
			fileDescriptors.get(fileNum).close();
			fileDescriptors.remove(fileNum);
			return 0;
		}
		return -1;
	}

	private int handleUnlink(int virtualAddress) { // part1: syscall unlink
		String filename = readVirtualMemoryString(virtualAddress, 256);
		if (filename != null && !filename.isEmpty() && 
				ThreadedKernel.fileSystem.remove(filename)) {
			return 0;
		}
		return -1;
	}
	
	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
			syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
			syscallRead = 6, syscallWrite = 7, syscallClose = 8,
			syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 * 
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td><tt>void halt();</tt></td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td><tt>void exit(int status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td><tt>int  join(int pid, int *status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td><tt>int  creat(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td><tt>int  open(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td><tt>int  read(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td><tt>int  write(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>8</td>
	 * <td><tt>int  close(int fd);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td><tt>int  unlink(char *name);</tt></td>
	 * </tr>
	 * </table>
	 * 
	 * @param syscall
	 *            the syscall number.
	 * @param a0
	 *            the first syscall argument.
	 * @param a1
	 *            the second syscall argument.
	 * @param a2
	 *            the third syscall argument.
	 * @param a3
	 *            the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
		case syscallHalt:
			return handleHalt();
			// part 3: syscallExit, syscallExec, syscallJoin
		case syscallExit:

			handleExit(a0);
		case syscallExec:

			return handleExec((char) a0, a1, (char) a2);
		case syscallJoin:

			return handleJoin(a0, a1);
			// /part1: syscallCreate, syscallOpen, syscallRead, syscallWrite,
			// syscallClose, syscallUnlink
		case syscallCreate:

			return handleCreate(a0);
		case syscallOpen:

			return handleOpen(a0);
		case syscallRead:

			return handleRead(a0, a1, a2);
		case syscallWrite:

			return handleWrite(a0, a1, a2);
		case syscallClose:

			return handleClose(a0);
		case syscallUnlink:

			return handleUnlink(a0);

		default:
			Lib.debug(dbgProcess, "Unknown syscall " + syscall);
			Lib.assertNotReached("Unknown system call!");
		}
		return 0;
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 * 
	 * @param cause
	 *            the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
		case Processor.exceptionSyscall:
			int result = handleSyscall(processor.readRegister(Processor.regV0),
					processor.readRegister(Processor.regA0),
					processor.readRegister(Processor.regA1),
					processor.readRegister(Processor.regA2),
					processor.readRegister(Processor.regA3));
			processor.writeRegister(Processor.regV0, result);
			processor.advancePC();
			break;

		default:
			Lib.debug(dbgProcess, "Unexpected exception: "
					+ Processor.exceptionNames[cause]);
			handleExit(-1);
			Lib.assertNotReached("Unexpected exception");
		}
	}
	
	// part3
	private int handleExec(char filePtr, int argc, char argvPtr) {

		String fileName = readVirtualMemoryString(filePtr, 256);
		if (fileName == null || !fileName.endsWith(".coff")) {
			return -1; // fileName must be read properly and have .coff extension, otherwise failure
		}

		byte[] argvArray = new byte[argc * 4];

		if (argc * 4 != readVirtualMemory(argvPtr, argvArray)) {
			return -1; // memory not read properly: failure, return -1
		}

		
		String[] args = new String[argc];
		for (int i = 0; i < argc; i++) { // loop through argvArray
			int ptr = Lib.bytesToInt(argvArray, i * 4); // get int value for ptr
			args[i] = readVirtualMemoryString(ptr, 256);
		}

		UserProcess child = new UserProcess();
		child.parent = this;
		children.put(child.processID, child);


		child.execute(fileName, args); // execute file with processed argv

		return child.processID;
	}

	private int handleJoin(int processID, int statusPtr) {
		UserProcess child = children.get(processID);
		if (child == null) { // processID does not correspond to direct child of this process
			return -1; 
		}
		while (!child.completed) {
			UserKernel.joinLock.acquire();
			UserKernel.waiting.sleep();
			UserKernel.joinLock.release();

		}

		children.remove(processID); // disown child
		if ((Integer) child.returnValue == null) {
			return 0;
		}
		writeVirtualMemory(statusPtr,
				Lib.bytesFromInt(exitstati.get(processID)));
		if (exitstati.get(processID) == -1) { // child R.V. is undefined: unhandled exception in exiting
			return 0;
		}
		return 1; // child exits normally: return 1

	}

	private void handleExit(Integer status) {
		UserKernel.joinLock.acquire();
		completed = true; // works with handleJoin()
		if (parent != null) {
			parent.recordChildExitStatus(processID, status);
		}

		for (UserProcess child : children.values()) {
			if (child != null) {
				child.parent = null; // disown all children: their parents now null
			}
		}
		children = null; // disown all children

		// close all open files
		for (int i = 0; i < fileDescriptors.size(); i++) {
			if (fileDescriptors.get(i) != null) {
				handleClose(i);
			}
		}
		unloadSections(); // free memory

		UserKernel.waiting.wakeAll();
		UserKernel.joinLock.release();
		staticLock.acquire(); 
		numProcesses--; // decrement numProcesses
		staticLock.release();
		if (numProcesses == 0) {
			Kernel.kernel.terminate(); // halt machine when last process exits
		}
		KThread.finish();
	}

	protected void recordChildExitStatus(int processID, int status) {

		exitstati.put(processID, status); // called by parent in exit(), records child exit status in hashmap
	}


	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 * 
	 * @param cause
	 *            the user exception that occurred.
	 */

	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;
	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = 8;

	private int initialPC, initialSP;
	private int argc, argv;

	private static final int pageSize = Processor.pageSize;
	private static final char dbgProcess = 'a';

	private static Lock staticLock = new Lock(); // lock for static variables

	public int returnValue;
	private static int nextProcessID = 0; // global variable, gives next process ID
	protected int processID; // this UserProcess' ID
	public boolean completed = false;
	protected UserProcess parent; // parent of this process
	// hashmap of this process' children and their IDs
	protected HashMap<Integer, UserProcess> children = new HashMap<Integer, UserProcess>();
	
	private HashMap<Integer, Integer> exitstati = new HashMap<Integer, Integer>();
	private static int numProcesses = 0; // to know when to halt machine in handleExit

	// // part1: hashmap for filedescriptors.
	private HashMap<Integer, OpenFile> fileDescriptors = new HashMap<Integer, OpenFile>();
	private int count = 1;
}
