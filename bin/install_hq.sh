cd ../data/apps/
wget https://github.com/ElasticHQ/elasticsearch-HQ/archive/v3.5.0.tar.gz
tar xfz v3.5.0.tar.gz
cd elasticsearch-HQ-3.5.0/
pip3 install -r requirements.txt
pip3 install flask_socketio
pip3 install apscheduler
pip3 install eventlet
python3 application.py
