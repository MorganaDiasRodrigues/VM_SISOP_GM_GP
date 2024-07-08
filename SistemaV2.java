import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class SistemaV2 {

    public static void main(String[] args) {
        SistemaV2 sistema = new SistemaV2(1024, 8);
        sistema.start();
    }

    // Funcao auxiliar para nao repetir os comandos no terminal
    public void help() {
        System.out.println("\n ---- COMANDOS DISPONIVEIS ----:\n" +
                "new <nomeDePrograma>: cria um processo na memória\n" +
                "rm <id>: retira o processo id do sistema, tenha ele executado ou nao\n" +
                "ps: lista todos processos existentes\n" +
                "dump <id>: lista o conteúdo do PCB e o conteúdo da memória do processo com id\n" +
                "dumpM <inicio, fim>: lista a memória entre posições início e fim, independente do processo\n" +
                "executa <id>: executa o processo com id fornecido. Se nao houver processo, retorna erro.\n" +
                "traceOn: liga modo de execuçao em que a CPU imprime cada instruçao executada\n" +
                "traceOff: desliga o modo acima\n" +
                "exit: sai do sistema\n" +
                "help: repete os comandos disponiveis\n" +
                "---------------------------------------------------------------------------\n" +
                "Digite um comando: ");
    }

    // -------------------------------------------------------------------------------------------------------
    // --------------------- H A R D W A R E - definicoes de HW
    // ----------------------------------------------

    // -------------------------------------------------------------------------------------------------------
    // --------------------- M E M O R I A - definicoes de palavra de memoria,
    // memória ----------------------

    public class Scheduler implements Runnable {
        private int ciclesLimit;
        private int processCurrentCicles = 0;
        private Queue<PCB> readyQueue;// Fila de processos prontos
        private Queue<PCB> blockedQueue;// Fila de processos bloqueados
        private Semaphore semCPU, semScheduler;

        public Scheduler(int ciclesLimit, Semaphore semCPU, Semaphore semScheduler) {
            this.ciclesLimit = ciclesLimit;
            this.semCPU = semCPU;
            this.semScheduler = semScheduler;
            this.readyQueue = new LinkedList<>();
            this.blockedQueue = new LinkedList<>();
        }

        public void addReadyProcess(PCB _pcb) {
            for (PCB pcb : readyQueue) {
                if (pcb.getId() == _pcb.getId()) {
                    System.out.println("Processo " + _pcb.getId() + " já na fila de prontos.");
                    return;
                }
            }
            _pcb.setRunning(false);
            readyQueue.add(_pcb);
        }

        public PCB getNextProcess() {
            return readyQueue.peek(); // Retorna o próximo processo sem removê-lo
        }

        public PCB removeNextProcess() {
            return readyQueue.poll(); // Remove e retorna o próximo processo
        }

        public boolean isEmpty() {
            return readyQueue.isEmpty();
        }

        public void addCicle() {
            processCurrentCicles++;
        }

        public int getProcessCurrentCicles() {
            return processCurrentCicles;
        }

        public boolean isLimit() {
            return processCurrentCicles == ciclesLimit;
        }

        public void resetCicles() {
            this.processCurrentCicles = 0;
        }

        public void addBlockProcess(PCB _pcb) {
            for (PCB pcb : blockedQueue) {
                if (pcb.getId() == _pcb.getId()) {
                    System.out.println("Processo " + _pcb.getId() + " já na fila de bloqueados.");
                    return;
                }
            }
            _pcb.setRunning(false);
            blockedQueue.add(_pcb);
        }

        public void unblockProcess(PCB _pcb) {
            if (blockedQueue.remove(_pcb)) {
                readyQueue.add(_pcb);
                System.out.println("Processo " + _pcb.getId() + " desbloqueado. Fila de prontos: " + getReadyQueue());
            } else {
                System.out.println("Processo " + _pcb.getId() + " não estava na fila de bloqueados.");
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    semScheduler.acquire();
                    if (!readyQueue.isEmpty()) {
                        PCB pcb = getNextProcess();
                        semCPU.release();
                        System.out.println("Próximo processo pronto para execução: " + pcb.getId());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public String getReadyQueue() {
            if (readyQueue.isEmpty()) {
                return "Nenhum processo pronto.";
            }
            return readyQueue.stream().map(_pcb -> String.valueOf(_pcb.getId())).collect(Collectors.joining(", "));
        }

        public String getBlockedQueue() {
            if (blockedQueue.isEmpty()) {
                return "Nenhum processo bloqueado.";
            }
            return blockedQueue.stream().map(_pcb -> String.valueOf(_pcb.getId())).collect(Collectors.joining(", "));
        }
    }


    public class Memory {
        public int tamMem;
        public Word[] m; // m representa a memória fisica: um array de posicoes de memoria (word)

        public Memory(int size) {
            tamMem = size;
            m = new Word[tamMem];
            for (int i = 0; i < tamMem; i++) {
                m[i] = new Word(Opcode.___, -1, -1, -1);
            }
        }

        public void dump(Word w) { // funcoes de DUMP nao existem em hardware - colocadas aqui para facilidade
            System.out.print("[ ");
            System.out.print(w.opc);
            System.out.print(", ");
            System.out.print(w.r1);
            System.out.print(", ");
            System.out.print(w.r2);
            System.out.print(", ");
            System.out.print(w.p);
            System.out.println("  ] ");
        }

        public void dump(int ini, int fim) {
            for (int i = ini; i < fim; i++) {
                System.out.print(i);
                System.out.print(":  ");
                dump(m[i]);
            }
        }
    }

    public class Word { // cada posicao da memoria tem uma instrucao (ou um dado)
        public Opcode opc; //
        public int r1; // indice do primeiro registrador da operacao (Rs ou Rd cfe opcode na tabela)
        public int r2; // indice do segundo registrador da operacao (Rc ou Rs cfe operacao)
        public int p; // parametro para instrucao (k ou A cfe operacao), ou o dado, se opcode = DADO

        public Word(Opcode _opc, int _r1, int _r2, int _p) { // vide definiçao da VM - colunas vermelhas da tabela
            opc = _opc;
            r1 = _r1;
            r2 = _r2;
            p = _p;
        }
    }

    public enum Opcode {
        DATA, ___, // se memoria nesta posicao tem um dado, usa DATA, se nao usada ee NULO ___
        JMP, JMPI, JMPIG, JMPIL, JMPIE, // desvios e parada
        JMPIM, JMPIGM, JMPILM, JMPIEM, STOP,
        JMPIGK, JMPILK, JMPIEK, JMPIGT,
        ADDI, SUBI, ADD, SUB, MULT, // matematicos
        LDI, LDD, STD, LDX, STX, MOVE, // movimentacao
        TRAP // chamada de sistema
    }

    public enum Interrupts { // possiveis interrupcoes que esta CPU gera
        noInterrupt, intEnderecoInvalido, intInstrucaoInvalida, intOverflow, intSTOP, intBlocked, intIO;
    }

    public class CPU implements Runnable {
        private int maxInt; // valores maximo e minimo para inteiros nesta cpu
        private int minInt;
        private int pc; // program counter
        private Word ir; // instruction register
        private int[] reg; // registradores da CPU
        private Interrupts irpt; // interrupção registrada
        private int base; // base de acesso na memoria
        private int limite; // limite de acesso na memoria
        private Memory mem; // memória
        private Word[] m; // referência à memória
        private InterruptHandling ih; // tratamento de interrupções
        private SysCallHandling sysCall; // tratamento de chamadas de sistema
        private boolean debug; // debug mode
        private Scheduler scheduler;
        private Semaphore semCPU, semScheduler;

        public CPU(Memory _mem, InterruptHandling _ih, SysCallHandling _sysCall, Scheduler _scheduler, Semaphore _semCPU, Semaphore _semScheduler, boolean _debug) {
            maxInt = 32767;
            minInt = -32767;
            mem = _mem;
            m = mem.m;
            reg = new int[10];
            ih = _ih;
            sysCall = _sysCall;
            scheduler = _scheduler;
            semCPU = _semCPU;
            semScheduler = _semScheduler;
            debug = _debug;
        }

        private boolean legal(int e) {
            return true;
        }

        private boolean testOverflow(int v) {
            if ((v < minInt) || (v > maxInt)) {
                irpt = Interrupts.intOverflow;
                return false;
            }
            return true;
        }

        public void setContext(int _base, int _limite, int _pc) {
            base = _base;
            limite = _limite;
            pc = _pc;
            irpt = Interrupts.noInterrupt;
        }

        public boolean run(Scheduler scheduler, PCB pcb) {
            while (true) {
                if (legal(pc)) {
                    ir = m[pc];
                    if (debug) {
                        System.out.println("                               pc: " + pc + "       exec: ");
                        mem.dump(ir);
                    }
                    switch (ir.opc) {
                        case LDI:
                            reg[ir.r1] = ir.p;
                            pc++;
                            break;
                        case LDD:
                            if (legal(ir.p)) {
                                reg[ir.r1] = m[ir.p].p;
                                pc++;
                            }
                            break;
                        case LDX:
                            if (legal(reg[ir.r2])) {
                                reg[ir.r1] = m[reg[ir.r2]].p;
                                pc++;
                            }
                            break;
                        case STD:
                            if (legal(ir.p)) {
                                m[ir.p].opc = Opcode.DATA;
                                m[ir.p].p = reg[ir.r1];
                                pc++;
                            }
                            break;
                        case STX:
                            if (legal(reg[ir.r1])) {
                                m[reg[ir.r1]].opc = Opcode.DATA;
                                m[reg[ir.r1]].p = reg[ir.r2];
                                pc++;
                            }
                            break;
                        case MOVE:
                            reg[ir.r1] = reg[ir.r2];
                            pc++;
                            break;
                        case ADD:
                            reg[ir.r1] = reg[ir.r1] + reg[ir.r2];
                            testOverflow(reg[ir.r1]);
                            pc++;
                            break;
                        case ADDI:
                            reg[ir.r1] = reg[ir.r1] + ir.p;
                            testOverflow(reg[ir.r1]);
                            pc++;
                            break;
                        case SUB:
                            reg[ir.r1] = reg[ir.r1] - reg[ir.r2];
                            testOverflow(reg[ir.r1]);
                            pc++;
                            break;
                        case SUBI:
                            reg[ir.r1] = reg[ir.r1] - ir.p;
                            testOverflow(reg[ir.r1]);
                            pc++;
                            break;
                        case MULT:
                            reg[ir.r1] = reg[ir.r1] * reg[ir.r2];
                            testOverflow(reg[ir.r1]);
                            pc++;
                            break;
                        case JMP:
                            pc = ir.p;
                            break;
                        case JMPIG:
                            if (reg[ir.r2] > 0) {
                                pc = reg[ir.r1];
                            } else {
                                pc++;
                            }
                            break;
                        case JMPIGK:
                            if (reg[ir.r2] > 0) {
                                pc = ir.p;
                            } else {
                                pc++;
                            }
                            break;
                        case JMPILK:
                            if (reg[ir.r2] < 0) {
                                pc = ir.p;
                            } else {
                                pc++;
                            }
                            break;
                        case JMPIEK:
                            if (reg[ir.r2] == 0) {
                                pc = ir.p;
                            } else {
                                pc++;
                            }
                            break;
                        case JMPIL:
                            if (reg[ir.r2] < 0) {
                                pc = reg[ir.r1];
                            } else {
                                pc++;
                            }
                            break;
                        case JMPIE:
                            if (reg[ir.r2] == 0) {
                                pc = reg[ir.r1];
                            } else {
                                pc++;
                            }
                            break;
                        case JMPIM:
                            pc = m[ir.p].p;
                            break;
                        case JMPIGM:
                            if (reg[ir.r2] > 0) {
                                pc = m[ir.p].p;
                            } else {
                                pc++;
                            }
                            break;
                        case JMPILM:
                            if (reg[ir.r2] < 0) {
                                pc = m[ir.p].p;
                            } else {
                                pc++;
                            }
                            break;
                        case JMPIEM:
                            if (reg[ir.r2] == 0) {
                                pc = m[ir.p].p;
                            } else {
                                pc++;
                            }
                            break;
                        case JMPIGT:
                            if (reg[ir.r1] > reg[ir.r2]) {
                                pc = ir.p;
                            } else {
                                pc++;
                            }
                            break;
                        case STOP:
                            irpt = Interrupts.intSTOP;
                            break;
                        case DATA:
                            irpt = Interrupts.intInstrucaoInvalida;
                            break;
                        case TRAP:
                            sysCall.handle(pcb);
                            irpt = Interrupts.intBlocked;
                            System.out.println("Processo " + pcb.getId() + " interrompido por chamada de sistema (TRAP).");
                            break;
                        default:
                            irpt = Interrupts.intInstrucaoInvalida;
                            System.out.println("Processo " + pcb.getId() + " interrompido por instrução inválida.");
                            break;
                    }
                    pcb.setPC(pc);
                }
                scheduler.addCicle();
                if (scheduler.isLimit()) {
                    pcb.setRunning(false);
                    irpt = Interrupts.intBlocked;
                    System.out.println("Processo " + pcb.getId() + " interrompido por atingir o limite de ciclos.");
                    scheduler.addReadyProcess(pcb); // Adiciona o PCB aos processos prontos
                    System.out.println("Processos na fila de prontos: " + scheduler.getReadyQueue());
                    pcb = scheduler.removeNextProcess(); // Remove o próximo processo da fila de prontos
                    System.out.println("Processo " + pcb.getId() + " agora pronto para execução.");
                    System.out.println("PC do processo atual: " + pcb.getPC());
                    setContext(0, mem.tamMem - 1, pcb.getPC());
                    pcb.setRunning(true);
                    scheduler.resetCicles();
                    continue; // Continua a execução com o próximo processo
                }
                if (irpt != Interrupts.noInterrupt) {
                    ih.handle(irpt, pc);
                    if (irpt == Interrupts.intSTOP) {
                        System.out.println("Processo " + pcb.getId() + " interrompido por STOP.");
                        return true;
                    } else if (irpt == Interrupts.intBlocked) {
                        scheduler.addBlockProcess(pcb);
                        System.out.println("Processo " + pcb.getId() + " bloqueado.");
                        System.out.println("Processos na fila de bloqueados: " + scheduler.getBlockedQueue());
                        System.out.println("Processos na fila de prontos: " + scheduler.getReadyQueue());
                        irpt = Interrupts.noInterrupt;
                        return false;
                    } else if (irpt == Interrupts.intIO) {
                        System.out.println("Processo " + pcb.getId() + " aguardando operação de I/O.");
                        irpt = Interrupts.noInterrupt; // Reseta a interrupção de I/O
                    }
                    break;
                }
            }
            return true;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    semCPU.acquire();
                    while (true) {
                        if (scheduler.isEmpty()) {
                            semScheduler.release();
                            break;
                        }
                        PCB pcb = scheduler.getNextProcess();
                        System.out.println("Rodando processo com ID: " + pcb.getId());
                        setContext(0, mem.tamMem - 1, pcb.getPC());
                        if (!run(scheduler, pcb)) {
                            scheduler.addReadyProcess(pcb);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class TabelaPaginas {
        private List<Integer> tabelaPagina;

        public TabelaPaginas(int numeroFrames) {
            tabelaPagina = new ArrayList<>(Collections.nCopies(numeroFrames, -1));
        }

        public boolean isFrameLivre(int index) {
            return index >= 0 && index < tabelaPagina.size() && tabelaPagina.get(index) == -1;
        }

        public void alocaFrame(int frameIndex) {
            if (frameIndex >= 0 && frameIndex < tabelaPagina.size()) {
                tabelaPagina.set(frameIndex, 1);
            }
        }

        public void liberaFrame(int frameIndex) {
            if (frameIndex >= 0 && frameIndex < tabelaPagina.size()) {
                tabelaPagina.set(frameIndex, -1);
            }
        }
    }

    public class GM {
        private Word[] m;
        private int tamPg;
        private int numeroFrames;
        private TabelaPaginas tabelaPaginas;
        public int[] framesAlocados;

        public GM(Word[] m, int tamPg) {
            this.m = m;
            this.tamPg = tamPg;
            this.numeroFrames = m.length / tamPg;
            this.tabelaPaginas = new TabelaPaginas(numeroFrames);
        }

        public List<Integer> getFramesDisponiveis() {
            List<Integer> framesLivres = new ArrayList<>();
            for (int i = 0; i < numeroFrames; i++) {
                if (tabelaPaginas.isFrameLivre(i)) {
                    framesLivres.add(i);
                }
            }
            return framesLivres;
        }

        public List<Integer> getFramesAlocados() {
            List<Integer> allocatedFramesList = new ArrayList<>();
            if (framesAlocados != null) {
                for (int frameIndex : framesAlocados) {
                    allocatedFramesList.add(frameIndex);
                }
            }
            return allocatedFramesList;
        }

        private void carregarInstrucoesNaMemoria(int indiceInicio, int indicePrograma, Word[] programa) {
            for (int indexMem = indiceInicio; indexMem < indiceInicio + tamPg; indexMem++) {
                if (indicePrograma >= programa.length || indexMem >= m.length)
                    break;
                m[indexMem] = new Word(
                        programa[indicePrograma].opc,
                        programa[indicePrograma].r1,
                        programa[indicePrograma].r2,
                        programa[indicePrograma].p);
                indicePrograma++;
            }
        }

        public boolean alocaPrograma(Word[] programa) {
            int numeroPalavras = programa.length;
            int numeroFramesNecessarios = (int) Math.ceil((double) numeroPalavras / tamPg);
            List<Integer> framesDisponiveis = getFramesDisponiveis();

            if (framesDisponiveis.size() < numeroFramesNecessarios) {
                System.out.println("Nao há frames suficientes para alocar o programa");
                return false;
            }

            framesAlocados = new int[numeroFramesNecessarios];
            int indicePrograma = 0;
            for (int index = 0; index < numeroFramesNecessarios; index++) {
                int frameIndexDisponivel = framesDisponiveis.get(index);
                tabelaPaginas.alocaFrame(frameIndexDisponivel);
                framesAlocados[index] = frameIndexDisponivel;

                int indexInicio = frameIndexDisponivel * tamPg;
                int numIntrucoes = Math.min(tamPg, numeroPalavras - indicePrograma);
                carregarInstrucoesNaMemoria(indexInicio, indicePrograma, programa);
                indicePrograma += numIntrucoes;
            }
            System.out.println("Programa alocado com sucesso. Total de frames alocados: " + numeroFramesNecessarios);
            return true;
        }

        public void desaloca(List<Integer> framesAlocados) {
            if (framesAlocados != null) {
                for (int frameIndex : framesAlocados) {
                    tabelaPaginas.liberaFrame(frameIndex);
                }
                System.out.println("Frames desalocados com sucesso.");
            } else {
                System.out.println("No frames were allocated or provided for deallocation.");
            }
        }
    }

    public class PCB {
        private int id;
        private int tamPrograma;
        private List<Integer> framesAlocados;
        private int pc;
        private boolean isRunning;

        public PCB(int id, List<Integer> framesAlocados, int tamPrograma) {
            this.id = id;
            this.framesAlocados = framesAlocados;
            this.tamPrograma = tamPrograma;
        }

        public void setRunning(boolean isRunning) {
            this.isRunning = isRunning;
        }

        public boolean isRunning() {
            return this.isRunning;
        }

        public void setPC(int pc) {
            this.pc = pc;
        }

        public int getPC() {
            return pc;
        }

        public List<Integer> getFramesAlocados() {
            return this.framesAlocados;
        }

        public int getId() {
            return this.id;
        }

        public int getTamPrograma() {
            return this.tamPrograma;
        }
    }

    public class GP {
        private final GM gm;
        private final Queue<PCB> filaProntos;
        private final Queue<PCB> filaBloqueados;
        private int processID;

        public GP(GM gm) {
            this.gm = gm;
            this.filaProntos = new LinkedList<>();
            this.filaBloqueados = new LinkedList<>();
            this.processID = 0;
        }

        public boolean criaProcesso(Word[] programa) {
            int tamanhoPrograma = programa.length;

            if (!gm.alocaPrograma(programa)) {
                System.out.println("Nao há memória suficiente para alocar o programa.");
                return false;
            }

            List<Integer> FramesAlocados = gm.getFramesAlocados();
            PCB pcb = new PCB(processID++, FramesAlocados, tamanhoPrograma);
            pcb.setPC(0);
            filaProntos.add(pcb);

            System.out.println("Processo criado com sucesso. ID do Processo: " + pcb.getId());
            return true;
        }

        public boolean desalocaProcesso(int id) {
            PCB toRemove = null;

            for (PCB pcb : filaProntos) {
                if (pcb.getId() == id) {
                    toRemove = pcb;
                    break;
                }
            }

            if (toRemove == null) {
                System.out.println("Processo com ID " + id + " nao encontrado.");
                return false;
            }
            gm.desaloca(toRemove.getFramesAlocados());

            filaProntos.remove(toRemove);

            System.out.println("Processo com ID " + id + " foi desalocado com sucesso.");
            return true;
        }

        public int getLastProcessId() {
            if (!filaProntos.isEmpty()) {
                return filaProntos.peek().getId();
            }
            return -1;
        }

        public int getProcessId(int id) {
            for (PCB pcb : filaProntos) {
                if (pcb.getId() == id) {
                    return pcb.getId();
                }
            }
            return -1;
        }

        public Queue<PCB> getFilaProntos() {
            return filaProntos;
        }

        public void unblockProcess(PCB pcb) {
            filaProntos.add(pcb);
        }

        public void blockProcess(PCB pcb) {
            filaBloqueados.add(pcb);
        }

        public PCB getNextBlockedProcess() {
            return filaBloqueados.poll();
        }
    }

    // ------------------- V M - constituida de CPU e MEMORIA
    public class VM {
        public int tamMem;
        public Word[] m;
        public Memory mem;
        public CPU cpu;
        public Scheduler scheduler;
        public Semaphore semCPU;

        public VM(int tamanhoMemoria, InterruptHandling ih, SysCallHandling sysCall, Scheduler scheduler, Semaphore semCPU, Semaphore semScheduler) {
            tamMem = tamanhoMemoria;
            mem = new Memory(tamMem);
            m = mem.m;
            cpu = new CPU(mem, ih, sysCall, scheduler, semCPU, semScheduler, true);
            this.scheduler = scheduler;
            this.semCPU = semCPU;
        }
    }

    public class InterruptHandling {
        public void handle(Interrupts irpt, int pc) {
            System.out.println("                                               Interrupcao " + irpt + "   pc: " + pc);
        }
    }

    public class SysCallHandling {
        private VM vm;

        public void setVM(VM _vm) {
            vm = _vm;
        }

        public void handle(PCB pcb) {
            System.out.println("                                               Chamada de SistemaV2 com op  /  par:  "
                    + vm.cpu.reg[8] + " / " + vm.cpu.reg[9]);
            vm.cpu.irpt = Interrupts.intBlocked;
            vm.scheduler.addBlockProcess(pcb);
            // Simular uma operação de IO
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // Simular tempo de IO
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                vm.scheduler.unblockProcess(pcb);
                vm.cpu.irpt = Interrupts.intIO;
                try {
                    vm.semCPU.release(); // Acorda a CPU para continuar a execução
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    public VM vm;
    public InterruptHandling ih;
    public SysCallHandling sysCall;
    public static Programas progs;
    public GM gm;
    public GP gp;
    public Scheduler scheduler;
    public Semaphore semCPU, semScheduler;
    public BlockingQueue<Integer> ioQueue;

    public SistemaV2(int tamanhoMemoria, int tamanhoPg) {
        ih = new InterruptHandling();
        sysCall = new SysCallHandling();
        semCPU = new Semaphore(0);
        semScheduler = new Semaphore(1);
        scheduler = new Scheduler(4, semCPU, semScheduler); // x ciclos por ciclo de cpu
        vm = new VM(tamanhoMemoria, ih, sysCall, scheduler, semCPU, semScheduler);
        sysCall.setVM(vm);
        progs = new Programas();
        gm = new GM(vm.m, tamanhoPg);
        gp = new GP(gm);
        ioQueue = new LinkedBlockingQueue<>();
    }

    public int createNewProcess(Word[] programa) {
        if (programa == null) {
            System.out.println("Programa nao encontrado.");
            return -1;
        }

        if (gp.criaProcesso(programa)) {
            return gp.getLastProcessId();
        } else {
            return -1;
        }
    }

    public void start() {
        Thread shellThread = new Thread(new ThreadShell(this));
        Thread cpuThread = new Thread(vm.cpu);
        Thread schedulerThread = new Thread(scheduler);
        Thread ioThread = new Thread(new IODevice(this));

        shellThread.start();
        cpuThread.start();
        schedulerThread.start();
        ioThread.start();
    }

    public class ThreadShell implements Runnable {
        private final SistemaV2 sistema;

        public ThreadShell(SistemaV2 sistema) {
            this.sistema = sistema;
        }

        @Override
        public void run() {
            Scanner scanner = new Scanner(System.in);
            sistema.help();
            while (true) {
                String command = scanner.nextLine();
                if (command.equals("help")) {
                    sistema.help();
                }

                if (command.equals("exit")) {
                    break;
                }

                if (command.startsWith("new")) {
                    String[] commandParts = command.split(" ");
                    String programaNome = commandParts[1];
                    switch (programaNome) {
                        case "fatorial" -> sistema.createNewProcess(progs.fatorial);
                        case "fibonacci10" -> sistema.createNewProcess(progs.fibonacci10);
                        case "progMinimo" -> sistema.createNewProcess(progs.progMinimo);
                        case "fatorialTRAP" -> sistema.createNewProcess(progs.fatorialTRAP);
                        case "PB" -> sistema.createNewProcess(progs.PB);
                        case "PC" -> sistema.createNewProcess(progs.PC);
                        case "fibonacciTRAP" -> sistema.createNewProcess(progs.fibonacciTRAP);
                        default -> System.out.println("Programa '" + programaNome + "' nao encontrado.");
                    }
                }

                if (command.startsWith("rm")) {
                    String[] commandParts = command.split(" ");
                    int id = Integer.parseInt(commandParts[1]);
                    sistema.gp.desalocaProcesso(id);
                }

                if (command.equals("ps")) {
                    if (sistema.gp.filaProntos.isEmpty()) {
                        System.out.println("\nNao ha processos na fila de prontos.");
                    } else {
                        System.out.println("\nProcessos prontos:");
                        for (PCB pcb : sistema.gp.filaProntos) {
                            System.out.println("ID: " + pcb.getId() + " - Tamanho do Programa: " + pcb.getTamPrograma());
                        }
                        System.out.println("\n");
                    }
                }

                if (command.startsWith("dump")) {
                    String[] commandParts = command.split(" ");
                    int id = Integer.parseInt(commandParts[1]);
                    for (PCB pcb : sistema.gp.filaProntos) {
                        if (pcb.getId() == id) {
                            System.out.println("\nPCB do processo " + id + ":");
                            System.out.println("ID: " + pcb.getId());
                            System.out.println("PC: " + pcb.getPC());
                            System.out.println("Tamanho do Programa: " + pcb.getTamPrograma());
                            System.out.println("Frames alocados " + pcb.getFramesAlocados());
                            System.out.println("Conteúdo da memória do processo " + id + ":");
                            sistema.vm.mem.dump(0, pcb.getTamPrograma());
                            System.out.println("\n");
                            break;
                        }
                    }
                }

                if (command.startsWith("dumpM")) {
                    String[] commandParts = command.split(" ");
                    int inicio = Integer.parseInt(commandParts[1]);
                    int fim = Integer.parseInt(commandParts[2]);
                    System.out.println("\n");
                    sistema.vm.mem.dump(inicio, fim);
                    System.out.println("\n");
                }

                if (command.startsWith("executa")) {
                    String[] commandParts = command.split(" ");
                    int id = Integer.parseInt(commandParts[1]);
                    if (sistema.gp.getProcessId(id) == -1) {
                        System.out.println("Processo com ID " + id + " nao encontrado.");
                    }
                    for (PCB pcb : sistema.gp.filaProntos) {
                        if (pcb.getId() == id) {
                            sistema.scheduler.addReadyProcess(pcb);
                            sistema.vm.cpu.setContext(0, sistema.vm.tamMem - 1, pcb.getPC());
                            sistema.vm.cpu.run(scheduler, pcb);
                            break;
                        }
                    }
                }

                if (command.equals("execAll")) {
                    for (PCB pcb : sistema.gp.filaProntos) {
                        sistema.scheduler.addReadyProcess(pcb);
                    }
                    System.out.println("Processos prontos: " + scheduler.getReadyQueue());
                    while (!sistema.scheduler.readyQueue.isEmpty()) {
                        PCB pcb = sistema.scheduler.readyQueue.poll();
                        sistema.vm.cpu.setContext(0, sistema.vm.tamMem - 1, pcb.getPC());
                        sistema.vm.cpu.run(scheduler, pcb);
                    }
                }

                if (command.equals("traceOn")) {
                    sistema.vm.cpu.debug = true;
                }

                if (command.equals("traceOff")) {
                    sistema.vm.cpu.debug = false;
                }
            }
            scanner.close();
        }
    }

    public class IODevice implements Runnable {
        private SistemaV2 sistema;

        public IODevice(SistemaV2 sistema) {
            this.sistema = sistema;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Integer ioRequest = sistema.ioQueue.take();
                    Thread.sleep(1000); // Simular tempo de IO
                    sistema.vm.cpu.irpt = Interrupts.intIO;
                    sistema.vm.semCPU.release(); // Acorda a CPU para continuar a execução
                    System.out.println("Operação de IO completa. CPU acordada.");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class Programas {
        public Word[] fatorial = new Word[]{
                new Word(Opcode.LDI, 0, -1, 5),    // Load immediate 5 in R0 (number to calculate factorial)
                new Word(Opcode.LDI, 1, -1, 1),    // Load immediate 1 in R1 (initial factorial value)
                new Word(Opcode.LDI, 6, -1, 1),    // Load immediate 1 in R6 (constant 1)
                new Word(Opcode.LDI, 7, -1, 20),   // Load immediate 20 in R7 (end of program address)
                new Word(Opcode.LDI, 8, -1, 11),   // Load immediate 11 in R8 (address to jump if done)
                new Word(Opcode.LDI, 9, -1, 15),   // Load immediate 15 in R9 (TRAP address)
                new Word(Opcode.MULT, 1, 0, -1),   // Multiply R1 by R0
                new Word(Opcode.SUB, 0, 6, -1),    // Decrement R0 by 1
                new Word(Opcode.JMPIG, 7, 0, -1),  // If R0 > 0, jump to address in R7
                new Word(Opcode.STD, 1, -1, 10),   // Store result from R1 in memory address 10
                new Word(Opcode.STOP, -1, -1, -1)  // STOP program
        };

        public Word[] fatorialTRAP = new Word[]{
                new Word(Opcode.LDI, 0, -1, 5),   // Load immediate 5 in R0 (number to calculate factorial)
                new Word(Opcode.LDI, 1, -1, 1),   // Load immediate 1 in R1 (initial factorial value)
                new Word(Opcode.LDI, 6, -1, 1),   // Load immediate 1 in R6 (constant 1)
                new Word(Opcode.LDI, 7, -1, 14),  // Load immediate 14 in R7 (end of program address)
                new Word(Opcode.LDI, 8, -1, 10),  // Load immediate 10 in R8 (address to jump if done)
                new Word(Opcode.MULT, 1, 0, -1),  // Multiply R1 by R0
                new Word(Opcode.SUB, 0, 6, -1),   // Decrement R0 by 1
                new Word(Opcode.JMPIG, 7, 0, -1), // If R0 > 0, jump to address in R7
                new Word(Opcode.STD, 1, -1, 10),  // Store result from R1 in memory address 10
                new Word(Opcode.TRAP, -1, -1, -1),// System call
                new Word(Opcode.STOP, -1, -1, -1) // STOP program
        };

        public Word[] fibonacci10 = new Word[]{
                new Word(Opcode.LDI, 0, -1, 10),   // Load immediate 10 in R0 (number to calculate Fibonacci)
                new Word(Opcode.LDI, 1, -1, 0),    // Load immediate 0 in R1 (first Fibonacci number)
                new Word(Opcode.LDI, 2, -1, 1),    // Load immediate 1 in R2 (second Fibonacci number)
                new Word(Opcode.LDI, 3, -1, 19),   // Load immediate 19 in R3 (end of program address)
                new Word(Opcode.LDI, 4, -1, 11),   // Load immediate 11 in R4 (address to jump if done)
                new Word(Opcode.LDI, 5, -1, 15),   // Load immediate 15 in R5 (TRAP address)
                new Word(Opcode.STD, 1, -1, 11),   // Store R1 in memory address 11
                new Word(Opcode.ADD, 2, 1, -1),    // R2 = R2 + R1
                new Word(Opcode.ADDI, 1, 1, 0),    // R1 = R1 + 1
                new Word(Opcode.SUBI, 0, 0, 1),    // R0 = R0 - 1
                new Word(Opcode.JMPIG, 3, 0, -1),  // If R0 > 0, jump to address in R3
                new Word(Opcode.STOP, -1, -1, -1)  // STOP program
        };

        public Word[] progMinimo = new Word[] {
                new Word(Opcode.LDI, 0, -1, 999),
                new Word(Opcode.STD, 0, -1, 10),
                new Word(Opcode.STD, 0, -1, 11),
                new Word(Opcode.STD, 0, -1, 12),
                new Word(Opcode.STD, 0, -1, 13),
                new Word(Opcode.STD, 0, -1, 14),
                new Word(Opcode.STOP, -1, -1, -1)
        };

        public Word[] PB = new Word[]{
                new Word(Opcode.LDI, 0, -1, 5),    // Load immediate 5 in R0
                new Word(Opcode.LDI, 1, -1, 1),    // Load immediate 1 in R1
                new Word(Opcode.LDI, 6, -1, 1),    // Load immediate 1 in R6
                new Word(Opcode.LDI, 7, -1, 20),   // Load immediate 20 in R7 (end of program address)
                new Word(Opcode.LDI, 8, -1, 11),   // Load immediate 11 in R8 (address to jump if done)
                new Word(Opcode.MULT, 1, 0, -1),   // Multiply R1 by R0
                new Word(Opcode.SUB, 0, 6, -1),    // Decrement R0 by 1
                new Word(Opcode.JMPIG, 7, 0, -1),  // If R0 > 0, jump to address in R7
                new Word(Opcode.STD, 1, -1, 10),   // Store result from R1 in memory address 10
                new Word(Opcode.TRAP, -1, -1, -1), // System call
                new Word(Opcode.STOP, -1, -1, -1)  // STOP program
        };

        public Word[] PC = new Word[]{
                new Word(Opcode.LDI, 0, -1, 5),    // Load immediate 5 in R0
                new Word(Opcode.LDI, 1, -1, 1),    // Load immediate 1 in R1
                new Word(Opcode.LDI, 6, -1, 1),    // Load immediate 1 in R6
                new Word(Opcode.LDI, 7, -1, 20),   // Load immediate 20 in R7 (end of program address)
                new Word(Opcode.LDI, 8, -1, 11),   // Load immediate 11 in R8 (address to jump if done)
                new Word(Opcode.MULT, 1, 0, -1),   // Multiply R1 by R0
                new Word(Opcode.SUB, 0, 6, -1),    // Decrement R0 by 1
                new Word(Opcode.JMPIG, 7, 0, -1),  // If R0 > 0, jump to address in R7
                new Word(Opcode.STD, 1, -1, 10),   // Store result from R1 in memory address 10
                new Word(Opcode.TRAP, -1, -1, -1), // System call
                new Word(Opcode.STOP, -1, -1, -1)  // STOP program
        };

        public Word[] fibonacciTRAP = new Word[]{
                new Word(Opcode.LDI, 0, -1, 10),   // Load immediate 10 in R0 (number to calculate Fibonacci)
                new Word(Opcode.LDI, 1, -1, 0),    // Load immediate 0 in R1 (first Fibonacci number)
                new Word(Opcode.LDI, 2, -1, 1),    // Load immediate 1 in R2 (second Fibonacci number)
                new Word(Opcode.LDI, 3, -1, 19),   // Load immediate 19 in R3 (end of program address)
                new Word(Opcode.LDI, 4, -1, 11),   // Load immediate 11 in R4 (address to jump if done)
                new Word(Opcode.LDI, 5, -1, 15),   // Load immediate 15 in R5 (TRAP address)
                new Word(Opcode.STD, 1, -1, 11),   // Store R1 in memory address 11
                new Word(Opcode.ADD, 2, 1, -1),    // R2 = R2 + R1
                new Word(Opcode.ADDI, 1, 1, 0),    // R1 = R1 + 1
                new Word(Opcode.SUBI, 0, 0, 1),    // R0 = R0 - 1
                new Word(Opcode.JMPIG, 3, 0, -1),  // If R0 > 0, jump to address in R3
                new Word(Opcode.TRAP, -1, -1, -1), // System call
                new Word(Opcode.STOP, -1, -1, -1)  // STOP program
        };
    }
}
