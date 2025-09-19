import os
import subprocess
import sys
from tkinter import Tk, filedialog, simpledialog, messagebox, StringVar, Label, Button, Frame

# ---------- GUI Setup ----------
root = Tk()
root.title("Easy Git Committer")
root.geometry("650x400")

selected_paths = []
status_text = StringVar()
status_text.set("Drag files/folders here or click 'Select Files/Folders'.")

# ---------- Logging Function ----------
def log(msg):
    status_text.set(msg)
    print(msg)
    root.update_idletasks()

# ---------- Select Files/Folders ----------
def select_files_folders():
    paths = filedialog.askopenfilenames(title="Select files/folders (you can select many)")
    if paths:
        for p in paths:
            if p not in selected_paths:
                selected_paths.append(p)
        log(f"Selected {len(selected_paths)} files/folders.")

# ---------- Commit & Push Function ----------
def commit_and_push():
    if not selected_paths:
        messagebox.showerror("Oops!", "No files or folders selected!")
        return

    # Ask for commit info
    name = simpledialog.askstring("Your Name", "Enter your name:", initialvalue="Local User")
    email = simpledialog.askstring("Your Email", "Enter your email:", initialvalue="local@example.com")
    message = simpledialog.askstring("Commit Message", "Enter your commit message:", initialvalue="My first commit")
    date = simpledialog.askstring("Commit Date", "Enter commit date (optional, like 2025-09-19T12:34:56):", initialvalue="")

    # Ask for GitHub info
    repo_url = simpledialog.askstring("GitHub Repo URL", "Enter your GitHub HTTPS repo URL (leave blank for local only):", initialvalue="")
    username, token = "", ""
    if repo_url:
        username = simpledialog.askstring("GitHub Username", "Enter your GitHub username:", initialvalue="")
        token = simpledialog.askstring("GitHub Token", "Enter your Personal Access Token:", show='*')

    # Determine folder to commit
    if len(selected_paths) == 1 and os.path.isdir(selected_paths[0]):
        folder_to_commit = selected_paths[0]
    else:
        # Copy all files into one folder for commit
        import tempfile, shutil
        temp_dir = tempfile.mkdtemp(prefix="git_temp_")
        for p in selected_paths:
            if os.path.isdir(p):
                shutil.copytree(p, os.path.join(temp_dir, os.path.basename(p)))
            else:
                shutil.copy2(p, temp_dir)
        folder_to_commit = temp_dir
        log(f"Copied files to temporary folder: {temp_dir}")

    # Initialize Git repo if not exists
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

    # Commit with optional date
    env = os.environ.copy()
    if date:
        env["GIT_AUTHOR_DATE"] = date
        env["GIT_COMMITTER_DATE"] = date

    log("Committing files...")
    subprocess.run(["git", "commit", "-m", message], cwd=folder_to_commit, env=env)

    # Push to GitHub if info provided
    if repo_url and username and token:
        log("Preparing to push to GitHub...")

        # Add credentials to HTTPS URL
        if repo_url.startswith("https://"):
            url_with_auth = repo_url.replace("https://", f"https://{username}:{token}@")
        else:
            url_with_auth = repo_url

        # Add remote
        subprocess.run(["git", "remote", "remove", "origin"], cwd=folder_to_commit, stderr=subprocess.DEVNULL)
        subprocess.run(["git", "remote", "add", "origin", url_with_auth], cwd=folder_to_commit)

        # Detect default branch automatically
        result = subprocess.run(["git", "ls-remote", "--symref", url_with_auth, "HEAD"], cwd=folder_to_commit, capture_output=True, text=True)
        branch = "main"
        if "refs/heads/" in result.stdout:
            line = result.stdout.splitlines()[0]
            branch = line.split("refs/heads/")[-1]
        log(f"Using branch: {branch}")

        # Set branch and push
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

# ---------- GUI Layout ----------
frame = Frame(root)
frame.pack(padx=20, pady=20, fill="both", expand=True)

Label(frame, textvariable=status_text, fg="green", wraplength=600).pack(pady=10)
Button(frame, text="Select Files/Folders", command=select_files_folders, width=25, bg="#2196F3", fg="white").pack(pady=10)
Button(frame, text="Commit & Push", command=commit_and_push, width=25, bg="#4CAF50", fg="white").pack(pady=10)

root.mainloop()
