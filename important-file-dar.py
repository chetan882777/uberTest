data=[]
flag=1
while(flag):
    a=int(input())
    if a>0:
        data.append(a)
    else:
        flag=0
def div7(num):
    if num%7==0:
        return True
    else:
        return False
def great100(num):
    if num>100:
        return True
    else:
        return False
data2=[]
for val in data:
    if div7(val) and great100(val):
        data2.append(-3)
    elif great100(val):
        data2.append(-2)
    elif div7(val):
        data2.append(-1)
    else:
        data2.append(val)
print(data2)
