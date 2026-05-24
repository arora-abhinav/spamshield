FROM python:3.14

#Sets working directory of the container to a new folder called app
WORKDIR /app

#Copies requirements.txt from current directory to container directory
COPY requirements.txt .

#Installs all dependancies required onto the image 
RUN pip install -r requirements.txt

#Copies everything else from the current working directory to the contianer working directory
#Includes code, datasets, json file including parameters
COPY . .

#Run when container starts
#--host 0.0.0.0 makes uvicorn listen on all network interfaces: allows a communication channel between the container and outside it
#--port 8000 listens for incomin traffic on port 8000
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]