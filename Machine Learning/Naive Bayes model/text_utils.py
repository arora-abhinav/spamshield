import re
import math

#This script contains all the utility functions that are common across the bernoulli naive bayes implementation 
#as well as the multinomial naive bayes implementation of a spam classifier

#Function to tokenize
def tokenize(text):
    text = text.lower()                   
    text = re.sub(r'[^a-z\s]', '', text)  
    tokens = text.split()                  
    return tokens

#Creating the vocablist using vocab_size words based on the highest occuring 
#word count
def build_vocab_list(items, x_train, train_tokens):
    counter_map = {}
    #Storing a counter of each word to keep the max vocab_size words occuring
    for i in range(len(x_train)):
        for token in train_tokens[i]:
            if token not in counter_map:
                counter_map[token] = 1
            else:
                counter_map[token] += 1
    
    #Sorts the map based on the frequency of the words occuring 
    sorted_map = sorted(counter_map.items(), key=lambda item:item[1], reverse=True)
    top_words = sorted_map[:items]

    final_map = {}
    counter = 0
    #counter is simply used to assign an index to the word so the index can be accessed
    #in O(1) time and so can the word be looked up in O(1) time
    for element in top_words:
        word, number = element
        final_map[word] = counter
        counter += 1
    
    return final_map


#Computes IDF for each word in the vocab list
#IDF(word) = log(total_documents / documents_containing_word)
#Called once at training time — IDF values are serialized to model_params.json
def build_idf(x_train, train_tokens, vocab_list):
    n = len(x_train)
    #Count how many documents contain each word
    doc_freq = {}
    for i in range(len(x_train)):
        #Use a set so each word is only counted once per document
        seen = set()
        for token in train_tokens[i]:
            if token in vocab_list and token not in seen:
                doc_freq[token] = doc_freq.get(token, 0) + 1
                seen.add(token)
    
    #Compute IDF for each word in vocab — stored by vocab index
    #Add 1 to denominator (Laplace smoothing) to avoid division by zero for unseen words
    idf = [0.0] * len(vocab_list)
    for word, idx in vocab_list.items():
        df = doc_freq.get(word, 0)
        idf[idx] = math.log(n / (df + 1))
    
    return idf


#Ratio of each class (for text, it is if the classes are spam or not)
def calculate_class_ratio(training_y):
    m = {}
    for i in range(len(training_y)):
        if training_y[i] not in m:
            m[training_y[i]] = 1
        else:
            m[training_y[i]] += 1
    
    return m

#Creating TF-IDF vectorized inputs for each training example in x_train
#TF(word, doc) = count of word in doc / total words in doc
#TF-IDF(word, doc) = TF(word, doc) * IDF(word)
#idf parameter is the list of IDF values computed by build_idf
#count parameter kept for backward compatibility — if False, uses binary (Bernoulli), if True uses TF-IDF
def vectorize(input_matrix, vocab_size, tokens, vocab_list, count, idf=None):
    vectorized_inputs = [[0.0] * vocab_size for _ in range(len(input_matrix))]
    for i in range(len(input_matrix)):
        total_tokens = len(tokens[i]) if len(tokens[i]) > 0 else 1
        for token in tokens[i]:
            if token in vocab_list:
                idx = vocab_list[token]
                if not count:
                    #Bernoulli — binary flag
                    vectorized_inputs[i][idx] = 1
                elif idf is not None:
                    #TF-IDF weighting
                    tf = vectorized_inputs[i][idx] / total_tokens
                    vectorized_inputs[i][idx] = (tf + (1 / total_tokens)) * idf[idx]
                else:
                    #Raw count fallback (original behavior)
                    vectorized_inputs[i][idx] += 1
        
    return vectorized_inputs


#Same as above but for a single message at inference time
#idf must be passed in — loaded from model_params.json
def vectorize_single(vocab_size, tokens, vocab_list, count, idf=None):
    vectorized_inputs = [0.0] * vocab_size
    total_tokens = len(tokens) if len(tokens) > 0 else 1
    for token in tokens:
        if token in vocab_list:
            idx = vocab_list[token]
            if not count:
                vectorized_inputs[idx] = 1
            elif idf is not None:
                #TF-IDF weighting
                tf = vectorized_inputs[idx] / total_tokens
                vectorized_inputs[idx] = (tf + (1 / total_tokens)) * idf[idx]
            else:
                vectorized_inputs[idx] += 1
        
    return vectorized_inputs