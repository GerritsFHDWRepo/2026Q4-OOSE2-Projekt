# 2026Q4-OOSE-2-Projekt
Projektarbeit im dritten Semester (2026Q4) während objektorientierte Softwaretechnik  2.

## Regeln/Richtlinien

1. Namensgebung für Branches bitte mit [Namen]-[Feature/Changes] benennen.
2. Pull Requests müssen von mindestens einer weiteren Person approved werden und sollten [Datum]-[Branchname] genannt werden.
3. Das package "docs" soll alle zukünftigen Dokumente wie Diagramme, Textdokumente oder Notizen zur Präsentation enthalten.

## Aufgabenstellung

Implementiert eine Variante des Erzeuger-Verbraucher-Systems, bei dem ihr den Buffer durch eine Warteschlange ersetzt,
die mehrere Werte nach dem FIFO-Prinzip zwischenspeichern kann.
Die Größe der Warteschlange soll durch einen Konstruktor-Parameter angegeben werden, sodass z.B. new Queue(5) eine
Queue erzeugt, die bis zu 5 Werte speichern kann. Ist die Warteschlange leer, sollte das Lesen blockiert werden;
ist sie voll, sollte das Schreiben blockiert werden. 
Eine Queue mit Größe 1 sollte sich verhalten wie der Buffer in der Vorlesung.
Diese Art der Warteschlange nennt man übrigens BlockingQueue und Java liefert im Paket java.util.concurrent mehrere Implementierungen davon aus.
Recherchiert online dazu. Geht davon aus, dass die Kapazität immer mindestens 1 ist.
Bei einem kleineren Wert soll eine IllegalArgumentException geworfen werden.