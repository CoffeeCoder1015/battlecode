@echo off
git checkout lecture
git fetch
git pull --allow-unrelated-histories
git checkout lect/main -- java
echo Sync complete, continue with `git add .` and 'git commit` to add the new changes