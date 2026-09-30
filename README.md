# Civil Services Officer Posting & Transfer Optimization System

A centralized administrative web application designed to streamline, automate, and bring transparency to the posting and transfer management of civil service officers.

---

## 📌 Project Overview
The **Civil Services Officer Posting & Transfer Optimization System** handles officer tenure management, transfer request submissions, vacancy matching, policy validation, committee reviews, and automated transfer order generation.

---

## ✨ Key Features & Modules

### 👤 1. Civil Services Officer Module
- **Profile Management**: View personal details, current cadre, grade, posting history, and tenure status.
- **Transfer Requests**: Submit transfer applications specifying preferred locations, reason (medical, spouse, tenure completion, representation), and supporting documentation.
- **My Requests & Status**: Track real-time progress of submitted transfer applications (Submitted, Under Review, Recommended, Approved, Rejected).
- **Notifications & Orders**: Receive real-time alerts on transfer order issuances and download transfer orders.

### 🛡️ 2. Cadre Administrator Module
- **Officer Records**: Manage officer profiles, cadre details, and tenure tracking.
- **Vacancy Management**: Monitor available positions across districts, departments, and designations.
- **Transfer Application Review**: Review submitted requests, verify attached documents, and run algorithmic optimization algorithms.
- **Transfer Order Processing**: Issue official Government Orders (G.O.) for approved transfers and automatically update officer postings.
- **Reports & Audit Logs**: Access comprehensive analytics, cadre distribution reports, and transparent audit logging.

### 🏛️ 3. Transfer Committee Module
- **Committee Dashboard**: Access pending high-priority and specialized transfer requests.
- **Evaluation & Review**: Review officer tenure, service record, vacancy suitability, and policy compliance.
- **Recommendations**: Submit formal recommendations (Approve / Reject with remarks) for final administrative decision.

---

## 🛠️ Technology Stack
- **Frontend**: HTML5, CSS3, JavaScript (Vanilla JS, Fetch API)
- **Backend**: Java Servlets (`javax.servlet.*`), JSP
- **Database**: MySQL 8.0 (JDBC Connection)
- **Web Server**: Apache Tomcat 9
- **Deployment Platform**: Docker / Vercel container integration

---

## 🗄️ Database Setup

1. Import the initial schema and demo data into MySQL:
   ```bash
   mysql -u root -p < database.sql
   ```
2. Database Name: `civil_services_transfer_system`

---

## ⚙️ Environment Configuration

The application automatically reads database credentials from environment variables, falling back to standard local defaults:

| Variable | Description | Local Default |
| :--- | :--- | :--- |
| `DB_HOST` | Database Hostname / Endpoint | `localhost` |
| `DB_PORT` | Database Port | `3306` |
| `DB_NAME` | MySQL Database Name | `civil_services_transfer_system` |
| `DB_USER` | MySQL Username | `root` |
| `DB_PASSWORD` | MySQL Password | `root` |

---

## 🚀 How to Run Locally

### Option A: Running on Apache Tomcat 9
1. Compile Java source files into `WEB-INF/classes`:
   ```bash
   javac -d WEB-INF/classes -cp "path/to/tomcat/lib/servlet-api.jar" src/com/example/*.java
   ```
2. Copy or deploy the application folder into Apache Tomcat's `webapps/` directory (e.g. `webapps/ROOT/` or `webapps/CivilServices/`).
3. Start Tomcat:
   ```bash
   bin/startup.sh   # Linux/macOS
   bin/startup.bat  # Windows
   ```
4. Access the web app at `http://localhost:8080/`.

---

## 🔑 Demo Accounts

| Role | Username | Password |
| :--- | :--- | :--- |
| **Cadre Administrator** | `admin` | `Admin@CS2026!` |
| **Civil Services Officer** | `officer1` | `Officer@CS2026!` |
| **Transfer Committee Member** | `committee1` | `Committee@CS2026!` |

---

## 🌐 Deployment Information
This repository is configured for containerized deployment on platforms supporting Docker containers (such as Vercel container runtime, Railway, Render, or AWS ECS/App Runner).
