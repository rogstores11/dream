import os, subprocess, sys
from tkinter import Tk, filedialog, simpledialog, messagebox

root = Tk()
root.withdraw()

folder = filedialog.askdirectory(title="Select folder to commit and push")
if not folder:
    sys.exit("No folder selected!")

name = simpledialog.askstring("Git Author", "Enter your name:", initialvalue="Local User")
email = simpledialog.askstring("Git Author", "Enter your email:", initialvalue="local@example.com")
message = simpledialog.askstring("Commit Message", "Enter commit message:", initialvalue="Commit from local script")
date = simpledialog.askstring("Commit Date", "Enter commit date (ISO):", initialvalue="")
repo_url = simpledialog.askstring("GitHub Repo URL", "Enter HTTPS GitHub repo URL:", initialvalue="")
username = simpledialog.askstring("GitHub Username", "Enter GitHub username:", initialvalue="")
token = simpledialog.askstring("GitHub Token", "Enter GitHub Personal Access Token:", show='*')

# Initialize git repo if needed
if not os.path.exists(os.path.join(folder, ".git")):
    subprocess.run(["git", "init"], cwd=folder)

# Configure local git author
subprocess.run(["git", "config", "user.name", name], cwd=folder)
subprocess.run(["git", "config", "user.email", email], cwd=folder)

# Add all files
subprocess.run(["git", "add", "--all"], cwd=folder)

# Commit with date
env = os.environ.copy()
if date:
    env["GIT_AUTHOR_DATE"] = date
    env["GIT_COMMITTER_DATE"] = date

subprocess.run(["git", "commit", "-m", message], cwd=folder, env=env)

# Push to GitHub
if repo_url:
    # Insert credentials into HTTPS URL
    if repo_url.startswith("https://"):
        url_with_auth = repo_url.replace(
            "https://", f"https://{username}:{token}@"
        )
    else:
        url_with_auth = repo_url

    # Add remote if not exists
    subprocess.run(["git", "remote", "remove", "origin"], cwd=folder, stderr=subprocess.DEVNULL)
    subprocess.run(["git", "remote", "add", "origin", url_with_auth], cwd=folder)
    
    result = subprocess.run(["git", "push", "-u", "origin", "master"], cwd=folder)
    if result.returncode == 0:
        messagebox.showinfo("Success", "✅ Pushed commit to GitHub!")
    else:
        messagebox.showerror("Error", "❌ Failed to push to GitHub. Check token/repo URL.")
else:
    messagebox.showinfo("Done", "✅ Committed locally (no remote push).")
