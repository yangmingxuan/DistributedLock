# DistributedLock
Distributed locks can be used in a variety of situations.
For example, when a user's money changes in a system, modify the record while preventing other processes or applications from being modified.
So lock processing before this operation is called, the name of the lock can only be for the user, only to get to the lock can be modified.
This locking method does not lock the table and affects the actions of other users.


分布式锁在多种场合下能使用。
比如系统里某用户的款项变化时，修改记录同时要防止其它进程或者应用也正在进行修改。
所以在调用此操作前进行加锁处理，锁的名字可以只针对该用户，只有获取到锁才可以修改。
用这种加锁方式不会锁表而影响其它用户的操作。
