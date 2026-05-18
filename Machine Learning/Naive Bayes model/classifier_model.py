import json
import heapq
import math
import text_utils
from fastapi import HTTPException, status

global data
with open("/Users/abhinavarora/Desktop/Android Spam Classifier/Machine Learning/Naive Bayes model/model_params.json") as params:
    data = json.load(params)

class_ratio_map = data["Class ratios"]
phi_dict = data["Phi dict"]
n_train = data["Train examples length"]
vocab_size = data["Vocab size"]
vocab_list = data["Vocab list"]
 

def predict_one(message):
    try:
        tokenized_msg = text_utils.tokenize(message)
        test_vector = text_utils.vectorize_single(vocab_size, tokenized_msg, vocab_list, True)
        heap = []
        heapq.heapify_max(heap)
        for element in class_ratio_map:
            s = 0
            for i in range(len(test_vector)):
                s += test_vector[i] * math.log(phi_dict[(str(element) + str(i))])
            heapq.heappush_max(heap, (math.log(class_ratio_map[element]/n_train) + s, element))
        
        return heap[0][1]
    except Exception:
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="Oops, something went wrong. Please report to developper")

def predict_multiple(messages):
    results = []
    for message in messages:
        results.append(predict_one(message))
    
    return results


    

