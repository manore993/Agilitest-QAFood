@echo off
@chcp 65001>nul
cmd /c "C:\Users\AishwaryaMANORE\Documents\AgilitestProjects\QAFOOD\ats.cmd" exec-report-3 ConstructionDeCommandeSuite1.xml ConstructionDeCommandeSuite2.xml ConstructionDeCommandeSuite3.xml ConstructionDeCommandeSuite4.xml > %~n0-logs.txt
start /b "" "C:\Users\AishwaryaMANORE\Documents\AgilitestProjects\QAFOOD\target\ats-output\ats-report.html"