# Custom LiveData: A Fun Experiment 🎯

Have you ever wondered why LiveData is lifecycle-aware? 🤔
The primary reasons include detaching observers, stopping UI updates when unnecessary, and more. But does it really work as advertised? Like, does it truly stop updating the UI when the lifecycle owner is paused or stopped?

The answer is yes — it stops updating the UI only when the lifecycle owner is destroyed, not just paused or stopped. Of course, this applies when you're using observe() with a LifecycleOwner and not observeForever().

Out of curiosity, I decided to create my own custom LiveData that stops updating the UI when the lifecycle owner is in the paused state. And guess what? It worked! 🎉

Here’s my experience.

## Challenges I Faced 🧗‍♂️

- Creating Custom LiveData
- Maintaining the Same ViewModel Instance Across Two Activities
- Visualizing Changes in Both Current and Previous Activities Simultaneously

## Creating Custom LiveData 🚀

To create custom LiveData, I copied the existing LiveData code into a new class, MyCustomLd, and modified it. The main challenge was determining when the lifecycle owner is paused, as the available lifecycle states are:
INITIALIZED ,CREATED,STARTED,RESUMED,DESTROYED
There’s no explicit paused state. To solve this, I introduced two variables: prevState and currentState.

The logic:

When the prevState is RESUMED and the currentState is STARTED, it indicates the lifecycle owner is paused.
I then used this condition to return false from the shouldBeActive() function, stopping the UI updates.
Although I’m unsure if this approach is optimal, it works as expected based on my understanding. ✅

## Sharing a ViewModel Between Two Activities 🔄

To achieve this, I considered dependency injection but decided to do it manually. Here’s what I did:

- Created a custom ViewModelStoreOwner as a singleton.
- Built a custom ViewModel factory.
- Used this custom owner and factory to instantiate the ViewModel.

Since the ViewModelStoreOwner is a singleton, the ViewModel instance remained the same across activities. To clean things up, I cleared the ViewModelStore in the onTerminate() method of the Application class.

This allowed me to observe my custom LiveData across both activities effortlessly. 🎯

## Visualizing Changes in Both Activities 👀

Initially, I thought of using a fragment to overlay the first activity. However, this didn’t work because the activity isn’t paused when a fragment is displayed over it.

To truly pause the activity, I needed to navigate to a new activity. But how would I see the first activity in this case?

Solution: I applied a TranslucentTheme to the second activity, making it completely transparent. This way, I could observe changes in both activities simultaneously!

## The Result 🎉

When transitioning from the first to the second activity:

- Custom LiveData: Stops UI updates in the first activity because it’s paused.
- Default LiveData: Continues updating the UI in the paused activity.

## Video 


https://github.com/user-attachments/assets/7c718389-f733-44be-9e40-1768062fd82a



## Takeaway:

I believe LiveData should ideally stop UI updates when the lifecycle owner is paused or stopped. This would ensure efficient resource usage and prevent unnecessary updates. 🌟

What do you think? Would you try building your own custom LiveData to explore these nuances? Let’s discuss! 🛠️
