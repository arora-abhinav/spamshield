import json
import heapq
import math
import text_utils
from fastapi import HTTPException, status
import os
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

global data
with open(os.path.join(BASE_DIR, "model_params.json")) as params:
    data = json.load(params)

class_ratio_map = data["Class ratios"]
phi_dict = data["Phi dict"]
n_train = data["Train examples length"]
vocab_size = data["Vocab size"]
vocab_list = data["Vocab list"]
idf = data["IDF"]
 

def predict_one(message):
    try:
        tokenized_msg = text_utils.tokenize(message)
        test_vector = text_utils.vectorize_single(vocab_size, tokenized_msg, vocab_list, True, idf)
        heap = []
        scores = {}
        heapq.heapify_max(heap)
        for element in class_ratio_map:
            s = 0
            for i in range(len(test_vector)):
                s += test_vector[i] * math.log(phi_dict[(str(element) + str(i))])

            score = math.log(class_ratio_map[element]/n_train) + s
            scores[element] = score 
            heapq.heappush_max(heap, (math.log(class_ratio_map[element]/n_train) + s, element))
        
        label = heap[0][1]

        # Softmax normalization for confidence
        max_score = max(scores.values())
        exp_scores = {k: math.exp(v - max_score) for k, v in scores.items()}
        total = sum(exp_scores.values())
        confidence = exp_scores[label] / total
        
        #Threshold that only shows a prediction of spam if it is really confident that it is a spam
        if label == "spam" and confidence < 0.88:
            label = "ham"

        return {"Classification": label, "Confidence": round(confidence, 4)}
    
    except Exception as e:
        print(e)
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="Oops, something went wrong. Please report to developper")

def predict_multiple(messages):
    results = []
    for message in messages:
        results.append(predict_one(message))
    
    return results

