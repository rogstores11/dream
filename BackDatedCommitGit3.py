import os, subprocess, sys, zipfile, tempfile
from tkinter import Tk, filedialog, simpledialog, messagebox, ttk, Text, Scrollbar, END
from tkinter import Frame, Label, Button, StringVar

# ---------- GUI Helpers ----------
root = Tk()
root.title("Git GUI Committer")
root.geometry("600x400")

folder_path = StringVar()
status_text = StringVar()
status_text.set("Select a folder to commit...")

def log(msg):
    status_text.set(msg)
    print(msg)
    root.update_idletasks()

# ---------- Select Folder ----------
def select_folder():
    path = filedialog.askdirectory(title="Select folder to commit/push")
    if path:
        folder_path.set(path)
        log(f"Selected folder: {path}")

# ---------- Main Commit & Push Function ----------
def commit_and_push():
    folder = folder_path.get()
    if not folder:
        messagebox.showerror("Error", "No folder selected!")
        return

    # Git author info
    name = simpledialog.askstring("Git Author", "Enter your name:", initialvalue="Local User")
    email = simpledialog.askstring("Git Author", "Enter your email:", initialvalue="local@example.com")
    message = simpledialog.askstring("Commit Message", "Enter commit message:", initialvalue="Commit from local GUI")
    date = simpledialog.askstring("Commit Date", "Enter commit date (ISO, e.g., 2025-09-19T12:34:56):", initialvalue="")

    # GitHub info
    repo_url = simpledialog.askstring("GitHub Repo URL", "Enter HTTPS GitHub repo URL (leave blank for local commit only):", initialvalue="")
    username, token = "", ""
    if repo_url:
        username = simpledialog.askstring("GitHub Username", "Enter GitHub username:", initialvalue="")
        token = simpledialog.askstring("GitHub Token", "Enter GitHub Personal Access Token:", show='*')

    # Initialize git repo if needed
    if not os.path.exists(os.path.join(folder, ".git")):
        log("Initializing git repository...")
        subprocess.run(["git", "init"], cwd=folder)

    # Configure git author
    log("Setting local git author...")
    subprocess.run(["git", "config", "user.name", name], cwd=folder)
    subprocess.run(["git", "config", "user.email", email], cwd=folder)

    # Add all files
    log("Adding all files...")
    subprocess.run(["git", "add", "--all"], cwd=folder)

    # Commit with specified date
    env = os.environ.copy()
    if date:
        env["GIT_AUTHOR_DATE"] = date
        env["GIT_COMMITTER_DATE"] = date

    log("Committing...")
    subprocess.run(["git", "commit", "-m", message], cwd=folder, env=env)

    # Push to GitHub if URL provided
    if repo_url and username and token:
        log("Preparing to push to GitHub...")
        if repo_url.startswith("https://"):
            url_with_auth = repo_url.replace("https://", f"https://{username}:{token}@")
        else:
            url_with_auth = repo_url

        # Remove existing origin if exists
        subprocess.run(["git", "remote", "remove", "origin"], cwd=folder, stderr=subprocess.DEVNULL)
        subprocess.run(["git", "remote", "add", "origin", url_with_auth], cwd=folder)

        # Detect default branch
        branch = "main"
        subprocess.run(["git", "branch", "-M", branch], cwd=folder)

        log("Pushing to GitHub...")
        result = subprocess.run(["git", "push", "-u", "origin", branch], cwd=folder)
        if result.returncode == 0:
            messagebox.showinfo("Success", "✅ Committed and pushed to GitHub!")
            log("Push successful!")
        else:
            messagebox.showerror("Error", "❌ Failed to push. Check token, repo URL, or branch.")
            log("Push failed.")
    else:
        messagebox.showinfo("Done", "✅ Committed locally (no remote push).")
        log("Local commit done.")

# ---------- GUI Layout ----------
frame = Frame(root)
frame.pack(padx=20, pady=20, fill="both", expand=True)

Label(frame, text="Folder to Commit:").pack(anchor="w")
Label(frame, textvariable=folder_path, fg="blue").pack(anchor="w")

Button(frame, text="Select Folder", command=select_folder).pack(pady=10)
Button(frame, text="Commit & Push", command=commit_and_push, bg="#4CAF50", fg="white").pack(pady=10)

Label(frame, textvariable=status_text, fg="green").pack(anchor="w", pady=10)

root.mainloop()
