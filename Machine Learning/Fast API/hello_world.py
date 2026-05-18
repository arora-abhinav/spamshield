from fastapi import FastAPI, Path, Query, HTTPException, status
#This is the library FastAPI recommends to have to have an option query parameter
from typing import Optional
from pydantic import BaseModel

#This is the object that the request body will have (the type). This inherits from the BaseModel class
#which essentialy is used to validate the type of data being sent as a request body
class Item(BaseModel):
    Name:str
    Brand:str
    #How FastAPI expects us to define optional types
    Fat:Optional[str] = None

#Created another put_item class that inherits from the BaseModel class. 
#This is for only optional parameters
class put_item(BaseModel):
    Name:Optional[str] = None
    Brand: Optional[str] = None
    Fat: Optional[str] = None
app = FastAPI()


inventory = {
    1:{"Name": "Milk",
       "Brand": "Amul",
       "Fat": "10g"},
    2:{
        "Name": "Chocolate",
        "Brand": "Cadbury",
        "Fat": "10g"
        }
    }

#Decorator must be there
#Brackets contain a route
@app.get("/")
#Function must be right below the HTTP method being programmed
def home():
    return {"Data": "Testing"}

@app.get("/get-item/{item_id}")
#This is called a path parameter. type hinting is used to tell fastapi which type it is to expect
#so it can automatically throw an error message if the incorrect type is put in
def get_item(item_id:int = Path(description="The id of the iterm to be viewed", gt=0, lt=5)):
    #the path function allows us to put some extra constraints on the path parameter being passed into the function
    if item_id in inventory:
        return inventory[item_id]
    return HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="item id not found")

#A Query parameter is something that isn't defined in the URL itself. It has the following format:
#instagram.com/redirect?name=Abhinav_Arora. Here, the redirect is defined in the URL, but nothing after that.
#The query parameter is defined after the ? mark and then the = sign is the passed in value of the query 
#parameter

@app.get("/get-name")
def get_name(name:str):
    for item_id in inventory:
        if inventory[item_id]["Name"] == name:
            return inventory[item_id]
    
    return HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="item name not found")


#This combines a query parameter and a path parameter
@app.get("/get-name/{item_id}")
#We cannot have an optional argument before a required argument so adding a * as the first argument allows 
#for that to be okay
#use Optinal[type] = value where type is the type of the parameter and value is what default 
#value should be used (if the parameter isn't entered) and the Optional keyword is used to indicate
#the parameter isn't required to be inputted
def get_name(*, name: Optional[str] = None, item_id:int):
    for item in inventory:
        if inventory[item]["Name"] == name:
            return inventory[item]
    
    return inventory[item_id]

#This would create a request body. This is how information is communicated to the webserver to add something
#in the database 
@app.post("/create-item/{item_id}")
def create_item(item_id: int, item: Item):
    #This shows one way to post something
    if item_id in inventory:
        return HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="item id already exists")
    inventory[item_id] = {"Name": item.Name, "Brand":item.Brand, "Fat":item.Fat}
    '''Or:
    inventory[item_id] = item
    #However, this would require the inventory dict in this case to be compatible with objects Item objects
    '''
    return inventory[item_id]

#This is the put method to update 
@app.put("/update-item/{item_id}")
def update_item(item_id:int, item:put_item):
    #This requires the item_id to be present in the update method 
    if item_id not in inventory:
        return HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="item id not found")
    
    #Since these are all optional parameters, we only wanna update the parameters if they actually exist
    if item.Name != None:
        inventory[item_id]["Name"] = item.Name
    if item.Brand != None:
        inventory[item_id]["Brand"] = item.Brand
    if item.Fat != None:
        inventory[item_id]["Fat"] = item.Fat

    return inventory[item_id]

#Deletes an item based off item_id
@app.delete("/delete-item")
#This is how to define a query parameter. The ... makes it so that it isn't optional an query parameter. Exlcuding that would make it optional
#Including the Optional[int] = Query() would make it so that None is a possible value
def delete_item(item_id:int = Query(..., description="Deleting an item from the inventory with the specified item_id")):
    if item_id not in inventory:
        return HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="item id not found")
    inventory.pop(item_id)
    return {"Data": "Removed"}