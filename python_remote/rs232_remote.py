import tkinter as tk
from tkinter import ttk, messagebox, simpledialog
import serial
import serial.tools.list_ports
import threading

# Mapping for special keys
KEYCODE_MAP = {
    37: 'Left', 38: 'Up', 39: 'Right', 40: 'Down',
    13: 'Enter', 188: ',',
}

class CustomMessageDialog(simpledialog.Dialog):
    """Custom dialog to allow default text selection and 'Enter' to submit"""
    def __init__(self, parent, title, initial_value):
        self.initial_value = initial_value
        super().__init__(parent, title)

    def body(self, master):
        self.label = ttk.Label(master, text="Enter English message:")
        self.label.pack(padx=10, pady=5)
        self.entry = ttk.Entry(master, width=40)
        self.entry.pack(padx=10, pady=5)
        self.entry.insert(0, self.initial_value)
        
        # Select all text and focus
        self.entry.selection_range(0, tk.END)
        self.entry.focus_set()
        return self.entry # Initial focus goes here

    def apply(self):
        self.result = self.entry.get()

class ProjectorRemote(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Projector RS232 Remote")
        self.geometry("450x550")
        self.ser = None
        self.keycode_bindings = {} 
        self.is_dialog_open = False # Flag to prevent wrong actions during input
        
        self.create_widgets()
        self.bind_all("<KeyPress>", self.handle_keypress)

    def create_widgets(self):
        # --- Communication Settings ---
        comm_frame = ttk.LabelFrame(self, text="Communication Settings")
        comm_frame.pack(padx=10, pady=10, fill="x")

        self.ports = [port.device for port in serial.tools.list_ports.comports()]
        self.com_var = tk.StringVar(self)
        if self.ports: self.com_var.set(self.ports[0])
        
        ttk.Label(comm_frame, text="Port:").grid(row=0, column=0, padx=5, pady=5)
        
        current_val = self.com_var.get() if self.ports else "N/A"
        self.com_menu = ttk.OptionMenu(comm_frame, self.com_var, current_val, *(self.ports if self.ports else ["N/A"]))
        self.com_menu.grid(row=0, column=1, padx=5, pady=5)
        
        self.connect_btn = ttk.Button(comm_frame, text="Connect", command=self.connect_serial)
        self.connect_btn.grid(row=0, column=2, padx=5, pady=5)
        
        self.disconnect_btn = ttk.Button(comm_frame, text="Disconnect", command=self.disconnect_serial, state="disabled")
        self.disconnect_btn.grid(row=0, column=3, padx=5, pady=5)

        # --- Remote Buttons ---
        remote_frame = ttk.LabelFrame(self, text="Remote Control")
        remote_frame.pack(padx=10, pady=5, fill="both", expand=True)
        btn_container = tk.Frame(remote_frame)
        btn_container.pack(expand=True)
        
        commands = [
            ("", "", None), ("↑", "00140 10", 38), ("", "", None),
            ("←", "00140 11", 37), ("Enter", "00140 12", 13), ("→", "00140 13", 39),
            ("", "", None), ("↓", "00140 14", 40), ("", "", None),
            ("Keystone +", "00140 15", 'k'), ("Vol +", "00140 18", 'f'), ("Brightness", "00140 19", 'b'),
            ("Keystone -", "00140 16", 188), ("Vol -", "00140 17", 'v'), ("Zoom", "00140 21", 'z'),
            ("AV Mute ON", "0002 1", 'x'), ("AV Mute OFF", "0002 0", 'c'), ("Menu", "00140 20", 'm'),
            ("Source", "00100 3", 'i'), ("Power ON", "0000 1", 'p'), ("Power OFF", "0000 0", 'o'),
            ("", "", None), ("Message", "CUSTOM_MSG", 'h'), ("", "", None),
        ]
        
        row, col = 0, 0
        for text, cmd, shortcut in commands:
            display_text = text
            if shortcut:
                if isinstance(shortcut, int):
                    self.keycode_bindings[shortcut] = cmd
                    display_text = f"{text} ({KEYCODE_MAP.get(shortcut, shortcut)})"
                else:
                    self.keycode_bindings[shortcut.lower()] = cmd
                    display_text = f"{text} ({shortcut.upper()})"
            
            if cmd == "":
                btn = ttk.Button(btn_container, text="", state="disabled")
            elif cmd == "CUSTOM_MSG":
                btn = ttk.Button(btn_container, text=display_text, command=self.prompt_message)
            else:
                btn = ttk.Button(btn_container, text=display_text, command=lambda c=cmd: self.send_command(c))
            
            btn.grid(row=row, column=col, padx=5, pady=5, ipadx=5, ipady=5)
            col += 1
            if col > 2: col = 0; row += 1

        self.status_label = ttk.Label(self, text="Status: Disconnected", relief="sunken", anchor="w")
        self.status_label.pack(side="bottom", fill="x")

    def prompt_message(self):
        """Show input box and pause global key listener"""
        self.is_dialog_open = True 
        dialog = CustomMessageDialog(self, "Send Message", "Hello World")
        if dialog.result:
            self.send_command(f"00210 {dialog.result}")
        self.is_dialog_open = False

    def handle_keypress(self, event):
        """Only process shortcuts if no dialog is open"""
        if self.is_dialog_open:
            return 

        # Priority: Exact Keycode then Keysym
        cmd = self.keycode_bindings.get(event.keycode) or self.keycode_bindings.get(event.keysym.lower())
        
        if cmd:
            if cmd == "CUSTOM_MSG":
                self.prompt_message()
            else:
                self.send_command(cmd)
    
    def connect_serial(self):
        try:
            port = self.com_var.get()
            if port == "N/A": return
            self.ser = serial.Serial(port=port, baudrate=9600, timeout=1)
            self.status_label.config(text=f"Status: Connected to {port}")
            self.connect_btn.config(state="disabled")
            self.disconnect_btn.config(state="normal")
        except Exception as e:
            messagebox.showerror("Error", f"Connection failed: {e}")

    def disconnect_serial(self):
        if self.ser: self.ser.close()
        self.ser = None
        self.status_label.config(text="Status: Disconnected")
        self.connect_btn.config(state="normal")
        self.disconnect_btn.config(state="disabled")

    def send_command(self, cmd):
        if self.ser and self.ser.is_open:
            try:
                self.ser.write(f"~{cmd}\r".encode('ascii'))
                self.status_label.config(text=f"Status: Sent '{cmd}'")
                threading.Thread(target=self.read_feedback, daemon=True).start()
            except Exception as e:
                self.status_label.config(text=f"Status: Send failed: {e}")
        else:
            messagebox.showwarning("Warning", "Not connected!")

    def read_feedback(self):
        try:
            res = self.ser.read(2).decode('ascii').strip()
            if res == 'P':
                msg = "Success (P)"
            elif res == 'F':
                msg = "Failed (F)"
            else:
                msg = f"Feedback: {res}" if res else "No Response"
            self.status_label.config(text=f"Status: {msg}")
        except:
            pass # Silent fail for feedback background thread

if __name__ == "__main__":
    app = ProjectorRemote()
    app.mainloop()