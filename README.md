1. Introduction
The Smart Budget App is a mobile financial management application developed using Kotlin in Android Studio. The application enables users to record expenses, set budget goals, analyse spending patterns, and monitor financial performance through visual reports and gamification features.

The purpose of the application is to help users gain better control over their finances by providing meaningful insights into spending behaviour and encouraging responsible budgeting habits.
 
2. Problem Statement
Many individuals struggle to manage their personal finances effectively due to a lack of visibility into their spending habits. Without consistent expense tracking, it becomes difficult to identify overspending, achieve savings goals, and make informed financial decisions.

The Smart Budget App addresses this challenge by providing users with an accessible platform for recording transactions, analysing spending patterns, and evaluating performance against predefined budget goals.

3. Project Objectives
• Provide a simple and intuitive interface for recording daily expenses.
• Enable users to categorise and review spending habits over time.
• Allow users to set minimum and maximum budget goals.
• Present spending data visually through graphs and reports.
• Encourage consistent financial tracking through gamification rewards and achievements.

4. Prototype Review and Improvements
The prototype established the core functionality of the application, including expense recording, budget goal creation, and transaction viewing.

Based on lecturer feedback, the registration process was enhanced through email and password validation. Invalid email formats are now rejected, password strength requirements are enforced, and descriptive validation messages provide immediate feedback to users. These improvements significantly strengthen application security and usability.

5. System Design
The application follows a layered architecture consisting of:

Presentation Layer: Activities, Fragments, and XML layouts responsible for user interaction.

Business Logic Layer: Handles expense calculations, budget comparisons, search functionality, and validation rules.

Data Layer: Uses Room Database, DAOs, and Entity classes to manage persistent storage and data retrieval.

Technology Stack:
• Kotlin
• Android Studio
• Room Database
• XML
• GitHub
• GitHub Actions
 
6. Application Features
Expense Management
• Add, view, and categorise expenses.
• Store transaction dates for reporting purposes.

Search and Filter Functionality
• Search transactions by description.
• Filter transactions by category.
• Combine search and filter criteria.

Edit and Delete Transactions
• Update transaction details.
• Remove incorrect records.

Budget Comparison
• Compare monthly and yearly spending trends.

Spending Graphs
• Visualise spending by category and time period.

Budget Goal Tracking
• Monitor spending against minimum and maximum targets.

Gamification
• Earn badges and rewards for maintaining positive financial habits.

7. User Interface Design
The user interface was designed according to four principles:

Simplicity: Screens remain uncluttered and task-focused.

Consistency: Typography, colours, and controls remain uniform throughout the application.

Ease of Navigation: Users can move between sections with minimal effort.

Clear Information Presentation: Graphs, reports, and badges improve readability and understanding.

8. Testing
Functional testing was conducted on all major application features.

Test Cases:
• Add Transaction – Passed
• Edit Transaction – Passed
• Delete Transaction – Passed
• Search Transaction – Passed
• Filter Transaction – Passed
• Budget Calculation – Passed

Automated tests were also implemented to verify database operations, business logic, and calculation accuracy.

9. Version Control and Continuous Integration
GitHub was used to manage source code, track changes, and maintain project history.

GitHub Actions was configured to:
• Build the application automatically.
• Execute automated tests.
• Detect integration issues early.

This ensured that the application remained stable and deployable throughout development.

10. Installation and Demonstration
Installation Process:
1. Download the APK file.
2. Install the APK on an Android device.
3. Launch the Smart Budget App.

Demonstration materials should include:
• Application walkthrough.
• Expense management features.
• Search and filter functionality.
• Budget tracking and reporting.
• Gamification features.

Add GitHub repository and demonstration video links before final submission.

11. Challenges Encountered
Key challenges included database integration, graph visualisation, user interface design, testing on physical devices, and configuring GitHub Actions.

These challenges were addressed through research, debugging, iterative testing, and continuous refinement of the application.


12. Screenshots
 
        
13. Conclusion
The Smart Budget App successfully delivers a comprehensive personal finance management solution. The final version improves significantly upon the original prototype through stronger validation, enhanced usability, additional transaction management features, and improved reporting capabilities.

The project demonstrates practical application of Android development, software architecture principles, local database management, version control, and continuous integration practices. Overall, the application meets its objectives and provides users with meaningful tools to improve financial awareness and budgeting behaviour.
<img width="451" height="172" alt="image" src="https://github.com/user-attachments/assets/923c7b63-0f4c-482e-ac32-130cc84ae07c" />


Authors
Mpho Muswede
Reamogetse Monegi
Funanani Ravhuhali
Jameel Makhomo

License
This project is for educational purposes.

YouTube Link

