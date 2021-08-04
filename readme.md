# Overview

In questo progetto:

* è presente un semplice "script" Java per generare dei dati CSV causali
* un breve tutorial - in forma di esercizio - finalizzato a mostrare l'uso di vari strumenti a riga di comando Unix per l'ispezione e l'elaborazione di file CSV (siano essi dati o logs)
    * nel caso dei logs, tenere presente che è sempre necessario definire a monte un minimo insieme di "best practices" tali per cui essi si prestino a essere facilmente elaborati mediante i tool descritti in questa pagina
    
## Perché usare istruzioni da riga di comando e non importare in Excel o in un database o usare altri strumenti di analisi dati (p.e. R)?

Sono tutti strumenti validi - e spesso _più_ validi  di quelli a riga di comando.

Ci sono però delle situazioni in cui è molto utile utilizzare - almeno come prima opzione - la riga di comando, p.e.:

* voglio fare dei conteggi relativamente semplici molto velocemente
* i dati stanno cambiando velocemente e voglio rifare molteplici volte i medesimi conteggi a piccola distanza temporale l'uno dall'altro
* voglio verificare diverse ipotesi sui dati velocemente

Tutto sta dunque nella velocità e comodità dello strumento.

## Perché è più veloce dell'uso p.e. di Excel?

La maggior parte delle analisi che ci è necessario fare può essere eseguita:

* filtrando i dati e qualificandoli in forma tabellare, con semantica esatta delle colonne ⇒ è un meccanismo base di grep + awk, che si ottiene memorizzando pochissimi concetti base e una semplice sintassi
* aggregando i dati ⇒ awk
* calcolando statistiche sui dati ⇒ datamash

# Sample data

Il file `data.csv` contiene dei log fittizi di un ipotetico software che:

* dato un file che rappresenta un documento testuale
* lo classifica
* scrive il tempo impiegato
* se la classificazione avviene con successo, scrive il tipo di documento identificato
* se occorre un errore, dà informazioni sull'errore

# Tasks

## Contare il numero di successi e fallimenti

Modalità "naif":

* ispeziono velocemente il file

```shell
head data.csv
```

* deduco i conteggi

```shell
cat data.csv | grep -ci success
cat data.csv | grep -ci failure
```

Modalità più "sistematica":

<details>
  <summary>Uso awk, sort e uniq</summary>

```shell
cat data.csv | awk -F\| '{print $6}' | sort | uniq -c
```
</details>

<details>
  <summary>Creo al volo anche il markdown sempre con awk</summary>

```shell
cat data.csv | awk -F\| '{print $6}' | sort | uniq -c | awk 'BEGIN {print "|outcome|count\n|---|---"}  {print "|" $2 "|" $1 }'
```

|outcome|count
|---|---
|FAILURE|20012
|SUCCESS|79988

</details>

<details>
  <summary>Aggiungo l'ordinamento su colonna usando sort</summary>

```shell
cat data.csv | awk -F\| '{print $6}' | sort | uniq -c | sort -k 1 -n -r |  awk 'BEGIN {print "|outcome|count\n|---|---"}  {print "|" $2 "|" $1 }'
```

outcome|count
|---|---
|SUCCESS|79988
|FAILURE|20012

</details>

<details>
  <summary>Takeaways</summary>

* awk:
    * mi permette di accedere alle "colonne" di una riga
    * devo definire il separatore con `-F` - e il più delle volte devo mettere l'escape char `\` - p.e. `-F\|`
    * faccio riferimento alla colonna i-esima con `$i`
    * il comando è sempre `'{ C-like command }'`
    * posso mettere comandi PRIMA dell'elaborazione delle righe e alla fine - p.e. `'BEGIN {prima} {singole righe} END {dopo}'`
* uniq
    * elimina le righe duplicate
    * se metto `-c` le conta ⇒ ha dunque l'effetto di una "group by" - MA devo prima fare `sort` perché le righe duplicate devo essere _consecutive_
* sort
    * ricordarsi che può ordinare dati tabellari con `-k <indice della colonna>`
 
</details>

## Contare il numero di documenti classificati per ogni tipo

<details>
  <summary>Come per il conteggio di successi/fallimenti, ma cambiando colonna e filtrando solo i successi</summary>

```shell
cat data.csv | grep -i success  | awk -F\| '{print $8}' | sort | uniq -c
```

|outcome|count
|---|---
|ATTO_DI_CITAZIONE|12130
|CONTRATTO|23942
|MEMORIA|31960
|PARERE|11956

</details>

<details>
  <summary>Aggiungiamo l'ordinamento per frequenza</summary>

```shell
cat data.csv | grep -i success  | awk -F\| '{print $8}' | sort | uniq -c | sort -k 1 -n -r
```

outcome|count
|---|---
|MEMORIA|31960
|CONTRATTO|23942
|ATTO_DI_CITAZIONE|12130
|PARERE|11956

</details>

<details>
  <summary>Aggiungiamo il tempo medio di processing per ogni tipo: dobbiamo usare gli array associativi</summary>

```shell
cat data.csv | grep -i success  | awk -F\| '{types_count[$8]++;millis[$8]+=$10} END {for (type in types_count) print type " " types_count[type] " " (millis[type]/types_count[type])}' | sort -k 3 -n -r
```

|outcome|count|mean time in millis
|---|---|---
|CONTRATTO|23942|350,241
|ATTO_DI_CITAZIONE|12130|350,134
|PARERE|11956|349,973
|MEMORIA|31960|347,779

</details>

<details>
  <summary>Il grep su success è impreciso: possiamo migliorare con il "pattern matching"</summary>

```shell
cat data.csv | awk -F\| '$6 ~ /SUCCESS/ {types_count[$8]++;millis[$8]+=$10} END {for (type in types_count) print type " " types_count[type] " " (millis[type]/types_count[type])}' | sort -k 3 -n -r
```

</details>

<details>
  <summary>Takeaways</summary>

* con awk posso analizzare qualsiasi dato espresso in forma tabellare
* per esigenze più complesse posso sostituire sort e uniq con array associativi

</details>

## Analizzare i tempi di esecuzione

<details>
  <summary>Troviamo il tempo medio di esecuzione - dei successi</summary>

```shell
cat data.csv | awk -F\| '$6 ~ /SUCCESS/ {total_millis+=$10;successes_count++} END {print total_millis/successes_count}'
```

```
279,319
```

Da notare che in una una precedente versione di questo script, invece che `successes_count` veniva usata la variabile built-in `NR`: tuttavia, `NR` dà il numero totale di righe processate, mentre in questo caso era necessario utilizzare il numero di righe che avevano sofddisfato il pattern match (numero per il quale non mi risulta esista una variabile built-in ad hoc).


</details>

Non è molto informativo. Possiamo senz'altro trovare la deviazione standard, ma sarebbe meglio avere anche la mediana…

Diventa complicato ed error prone.

Usiamo invece `datamash`.

<details>
  <summary>Troviamo max, mean e median tempo di esecuzione</summary>

```shell
cat data.csv | awk -F\| '$6 ~ /SUCCESS/ {print $10}' | datamash max 1 mean 1 median 1
```

```
1596	349,2010426564	316
```

</details>

<details>
  <summary>Aggiungiamo gli headers in modo che l'output sia più chiaro</summary>

```shell
cat data.csv | awk -F\| 'BEGIN {print "millis"} $6 ~ /SUCCESS/ {print $10}' | datamash -H max 1 mean 1 median 1
```

```
max(millis)	mean(millis)	median(millis)
1596	349,2010426564	316
```

</details>

<details>
  <summary>Vogliamo più informazioni: troviamo decili, e il 99esimo percentile</summary>

```shell
cat data.csv | awk -F\| 'BEGIN {print "millis"} $6 ~ /SUCCESS/  {print $10}' | datamash  -H perc:10 1 perc:20 1 perc:30 1 perc:40 1 perc:50 1 perc:60 1 perc:70 1 perc:80 1 perc:90 1 perc:99 1
```

```
perc:10(millis)	perc:20(millis)	perc:30(millis)	perc:40(millis)	perc:50(millis)	perc:60(millis)	perc:70(millis)	perc:80(millis)	perc:90(millis)	perc:99(millis)
62	123	186	250	316	384	460	553	683	998
```

</details>

<details>
  <summary>Un'immagine vale più di mille parole: usiamo gnuplot nel terminale per fare il plotting dei dati</summary>

```shell
cat data.csv | awk -F\| '$6 ~ /SUCCESS/  {print $10}' | sort -n -r | gnuplot -e 'set term dumb; pl "-" pt "*"'
```

```
  1600 +-------------------------------------------------------------------+   
       |        +       +        +       +        +       +        +       |   
  1400 |-+                                                     "-"    *  +-|   
       |                                                                   |   
       |                                                                   |   
  1200 |-+                                                               +-|   
       |                                                                   |   
  1000 |*+                                                               +-|   
       |*                                                                  |   
       |**                                                                 |   
   800 |-***                                                             +-|   
       |   ****                                                            |   
   600 |-+    *****                                                      +-|   
       |          ******                                                   |   
       |               *******                                             |   
   400 |-+                   *********                                   +-|   
       |                             *********                             |   
   200 |-+                                   *********                   +-|   
       |                                             **********            |   
       |        +       +        +       +        +       +   **********   |   
     0 +-------------------------------------------------------------------+   
       0      10000   20000    30000   40000    50000   60000    70000   80000 
```

</details>

<details>
  <summary>Possiamo anche usare la groupby e avere dati semplici e precisi sui tempi per ogni tipo</summary>

```shell
cat data.csv | awk -F\| 'BEGIN {print "type\tmillis"} $6 ~ /SUCCESS/  {print $8 "\t" $10}' | datamash --sort -H groupby 1 mean 2 median 2 q1 2 q3 2 perc:99 2
```

```
GroupBy(type)	mean(millis)	median(millis)	q1(millis)	q3(millis)	perc:99(millis)
ATTO_DI_CITAZIONE	350,13380049464	317,5	155	507	996
CONTRATTO	350,24074847548	317	157	503	997
MEMORIA	347,77934918648	315	153	503	990
PARERE	349,97306791569	315	155	502	1020
```

Da notare che per separare le colonne dobbiamo usare `\t` nei comandi awk.

</details>

<details>
  <summary>Takeaways</summary>

* con datamash posso fare qualsiasi tipo di analisi statistica su dati tabellari
* awk può essere usato per "preparare" i dati per datamash
* con datamash possiamo usare la groupby (anche su più di una colonna
  )

</details>

## Analizziamo i dati mediant SQL - ma dalla linea di comando

<details>
  <summary>Con AWK trasformiamo il log in "forma tabellare" CSV</summary>

```shell
cat data.csv | awk -F\| 'BEGIN {print "index,outcome,type,error,millis"} {isSuccess = $6 == "SUCCESS"; print $3 "," $6 "," ((isSuccess) ? $8 : "") "," ((!isSuccess) ? $8 : "") "," $10}'
```

Il risultato sarà del tipo:

```
index,outcome,type,error,millis
0,SUCCESS,PARERE,,426
1,SUCCESS,MEMORIA,,244
2,SUCCESS,MEMORIA,,467
3,FAILURE,,FILE_NOT_FOUND,67
4,SUCCESS,MEMORIA,,641
5,SUCCESS,ATTO_DI_CITAZIONE,,237
6,FAILURE,,FILE_NOT_FOUND,113
7,SUCCESS,ATTO_DI_CITAZIONE,,203
8,SUCCESS,MEMORIA,,617
…
```

Provvediamo per comodità a scrivere quanto sopra in un file `data_tabular.csv`:

```shell
cat data.csv | awk -F\| 'BEGIN {print "index,outcome,type,error,millis"} {isSuccess = $6 == "SUCCESS"; print $3 "," $6 "," ((isSuccess) ? $8 : "") "," ((!isSuccess) ? $8 : "") "," $10}' > data_tabular.csv
```


</details>

<details>
  <summary>Usiamo csvkit https://csvkit.readthedocs.io/en/latest/ per importare "al volo" il file CSV in una DB SQLite</summary>

```shell
csvsql --db sqlite:///data.db --insert data_tabular.csv
```

Il database verrà creato in un file `data.db` che sarà scritto nel folder in cui il comando viene eseguito.

</details>

<details>
  <summary>Ora possiamo interrogare il DB con statements SQL: p.e. interroghiamo il database per ottenere i tempi medi per ogni errore</summary>

```shell
sql2csv --db sqlite:///data.db --query "select error, avg(millis) from data_tabular where error is not null group by error"
```

Otterremo questo output:

```
error,avg(millis)
CANNOT_OPEN_FILE,344.4945689069925
FILE_NOT_FOUND,346.442634164542
UNSUPPORTED_ENCODING,344.78375634517766
```

</details>

<details>
  <summary>Takeaways</summary>

* csvkit è una "suite" di tools da riga di comando per manipolare, analizzare e interrogare file CSV - eventualmente con l'ausilio di SQL
* un file di log (in un formato "standard") mediante awk può essere trasformato in un insieme di dati tabellari che possono essere importati in un db SQLite
* a questo punto posso interrogare direttamente i dati mediante SQL usando sempre csvkit

</details>
