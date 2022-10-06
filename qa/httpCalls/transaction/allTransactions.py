import json

# execute a transaction/allTransactions call (list of all mempool transactions)
def allTransactions(sidechainNode, format = True):
    j = {"format": format }
    request = json.dumps(j)
    response = sidechainNode.transaction_allTransactions(request)
    if "error" in response:
        print(response)
    else:
        return response["result"]