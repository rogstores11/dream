import os, subprocess, sys
from tkinter import Tk, filedialog, simpledialog

# Initialize Tkinter
root = Tk()
root.withdraw()  # hide main window

# Ask for folder to commit
folder = filedialog.askdirectory(title="Select folder to commit")
if not folder:
    sys.exit("No folder selected!")

# Ask for git info
name = simpledialog.askstring("Git Author", "Enter your name:", initialvalue="Local User")
email = simpledialog.askstring("Git Author", "Enter your email:", initialvalue="local@example.com")
message = simpledialog.askstring("Commit Message", "Enter commit message:", initialvalue="Commit from local script")
date = simpledialog.askstring("Commit Date", "Enter commit date (ISO, e.g., 2025-09-19T12:34:56):", initialvalue="")

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

print("✅ Commit done in folder:", folder)
