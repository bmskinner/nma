REM Render the user guide from markdown
Rscript -e "bookdown::render_book('index.Rmd', clean = FALSE)"

REM Copy the user guide to the target folder for inclusion in the jar
Xcopy /E /I ".\\_book" "..\\..\\target\\classes\\user-guide"

REM Delete the existing installed user guide in the nma folder so we can
REM test if the packaged version is loading properly
rmdir /Q /S ${user.home}\\.nma\\user_guide_v${project.version}