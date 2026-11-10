Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  Starting J.A.R.V.I.S. Spring Boot Backend on Port 8080" -ForegroundColor Yellow
Write-Host "========================================================" -ForegroundColor Cyan
Set-Location -Path "$PSScriptRoot\Backend\jarvis-backend"
.\mvnw.cmd spring-boot:run
