1: 检查是否存在~/.ssh文件夹, 文件夹下是否存在id_rsa, id_rsa.pub两个文件, 若存在直接id_rsa.pub绑定github即可
2: 使用ssh-keygen -t rsa -C "your_email@example.com" 生成sshkey
