readme file

Pietro Lessa, Luiza Nunes, Morgana Dias, Adriana Serpa, Giuliano Barbosa 

seção implementação: 
Nosso programa implementa o scheduler por interrupção a cada 4 instruções.

O programa tem um interrupt handler de 6 tratamentos de rotina diferentes:
- intIO, intCycle, intBlocked, intOverflow, intInstrucaoInvalida, intSTOP 

Os programas tem estados de ready, blocked e running e são usados nas rotinas de interrupção. 
Temos as filas de blocked, e ready com os programas gerenciados pelo scheduler 

Os programas conseguem ser criados constantemente sem necessidade de reiniciar o sistema 

Os programas são desalocados da memoria quando há uma interrupção de stop e quando há uma interrupção de instrução inválida. 



O programa tem 4 threads e cada uma é responsável por deixar o programa mais eficiente. São elas: 
ThreadShell que é responsável por interagir com o usuário e criar programas enquanto espera comandos. 


CPU que é uma thread e executa as instruções dos programas e as demais operações de ciclo de CPU 

Scheduler que faz os escalonamentos por fatia de instrução

Thread IO que deveria interromper a CPU conforme pedidos de leitura e escrita. Não está completa. 


Sessão de testes: 
Teste 1 - criar 2 programas sem trap com o comando new e dar execAll e.g. new progMinimo, new fatorial, execAll
resultado esperado : que se execute o programa com o escalonamento de 4 em 4 instruções até seu fim ou uma instrução invalida. 

Teste 2 - criar 1 programa sem trap e outro programa com trap. e.g. new progMinimo, new fatorialTRAP, execAll 
resultado esperado: quando executar o fatorial trap chama-se a rotina de tratamento de intBlocked. 

Teste 3 - criar o programa progMinimo, dar dumpM 0 30, dar execAll e dar dumpM 0 30 no final . No final só terá resultados de data das operações mas nenhuma instrução. 

