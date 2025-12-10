# Запуск backend
Start-Process powershell -ArgumentList "cd C:\App\APP\backend; mvn clean package -q; java -jar target/study-group-backend-1.0.0-jar-with-dependencies.jar"

# Запуск frontend
Start-Process powershell -ArgumentList "cd C:\App\APP\frontend; npm run dev"
