import os
import subprocess
import tempfile
import shutil
from tkinter import Tk, simpledialog, messagebox, StringVar, Label, Frame

# GUI setup
root = Tk()
root.title("Drag-and-Drop Git Committer")
root.geometry("700x400")
status_text = StringVar()
status_text.set("Drag files or folders onto this window to commit and push!")

def log(msg):
    status_text.set(msg)
    print(msg)
    root.update_idletasks()

def handle_drop(event):
    # On Windows paths are separated by space
    paths = root.tk.splitlist(event.data)
    commit_and_push(paths)

def commit_and_push(paths):
    if not paths:
        messagebox.showerror("Error", "No files or folders provided!")
        return

    # Ask for commit info
    name = simpledialog.askstring("Your Name", "Enter your name:", initialvalue="Local User")
    email = simpledialog.askstring("Your Email", "Enter your email:", initialvalue="local@example.com")
    message = simpledialog.askstring("Commit Message", "Enter your commit message:", initialvalue="My commit")
    date = simpledialog.askstring("Commit Date", "Enter commit date (optional, like 2025-09-19T12:34:56):", initialvalue="")

    # Ask for GitHub info
    repo_url = simpledialog.askstring("GitHub Repo URL", "Enter HTTPS GitHub repo URL (leave blank for local commit only):", initialvalue="")
    username, token = "", ""
    if repo_url:
        username = simpledialog.askstring("GitHub Username", "Enter your GitHub username:", initialvalue="")
        token = simpledialog.askstring("GitHub Token", "Enter your Personal Access Token:", show='*')

    # Determine folder for commit
    if len(paths) == 1 and os.path.isdir(paths[0]):
        folder_to_commit = paths[0]
    else:
        temp_dir = tempfile.mkdtemp(prefix="git_temp_")
        for p in paths:
            if os.path.isdir(p):
                shutil.copytree(p, os.path.join(temp_dir, os.path.basename(p)))
            else:
                shutil.copy2(p, temp_dir)
        folder_to_commit = temp_dir
        log(f"Copied files to temporary folder: {temp_dir}")

    # Initialize git repo
    if not os.path.exists(os.path.join(folder_to_commit, ".git")):
        log("Initializing Git repository...")
        subprocess.run(["git", "init"], cwd=folder_to_commit)

    # Set author info
    log("Setting author info...")
    subprocess.run(["git", "config", "user.name", name], cwd=folder_to_commit)
    subprocess.run(["git", "config", "user.email", email], cwd=folder_to_commit)

    # Add all files
    log("Adding files...")
    subprocess.run(["git", "add", "--all"], cwd=folder_to_commit)

    # Commit
    env = os.environ.copy()
    if date:
        env["GIT_AUTHOR_DATE"] = date
        env["GIT_COMMITTER_DATE"] = date
    log("Committing files...")
    subprocess.run(["git", "commit", "-m", message], cwd=folder_to_commit, env=env)

    # Push to GitHub if info provided
    if repo_url and username and token:
        log("Preparing to push to GitHub...")
        url_with_auth = repo_url.replace("https://", f"https://{username}:{token}@") if repo_url.startswith("https://") else repo_url
        subprocess.run(["git", "remote", "remove", "origin"], cwd=folder_to_commit, stderr=subprocess.DEVNULL)
        subprocess.run(["git", "remote", "add", "origin", url_with_auth], cwd=folder_to_commit)

        # Detect default branch
        result = subprocess.run(["git", "ls-remote", "--symref", url_with_auth, "HEAD"], cwd=folder_to_commit, capture_output=True, text=True)
        branch = "main"
        if "refs/heads/" in result.stdout:
            line = result.stdout.splitlines()[0]
            branch = line.split("refs/heads/")[-1]
        log(f"Using branch: {branch}")

        subprocess.run(["git", "branch", "-M", branch], cwd=folder_to_commit)
        log("Pushing to GitHub...")
        push_result = subprocess.run(["git", "push", "-u", "origin", branch], cwd=folder_to_commit)
        if push_result.returncode == 0:
            messagebox.showinfo("Success", "✅ Committed and pushed to GitHub!")
            log("Push successful!")
        else:
            messagebox.showerror("Error", "❌ Push failed. Check token, username, or repo URL.")
            log("Push failed.")
    else:
        messagebox.showinfo("Done", "✅ Committed locally (no GitHub push).")
        log("Local commit done.")

# GUI layout
frame = Frame(root)
frame.pack(padx=20, pady=20, fill="both", expand=True)
Label(frame, textvariable=status_text, fg="green", wraplength=650).pack(pady=10)

# Drag-and-drop binding
# try:
#     import tkinterdnd2 as tkdnd
#     dnd_root = tkdnd.TkinterDnD.Tk()
#     dnd_root.drop_target_register(tkdnd.DND_FILES)
#     dnd_root.dnd_bind('<<Drop>>', handle_drop)
#     log("Drag-and-drop ready!")
# except ImportError:
#     log("tkinterdnd2 not installed — drag-and-drop won't work. Use manual selection.")

root.mainloop()
