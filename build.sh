echo 'Installing container for fncore'
docker build -t fncore .
echo 'Run with ./fncore. It will run the app on port 9120.'
echo 'Make sure no other application is using that port'