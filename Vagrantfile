# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
  config.vm.hostname = "offerready-xslt-library"
  config.vm.box = "ubuntu/xenial64"
  
  if not Vagrant::Util::Platform.windows? then
    config.vm.synced_folder "~/.m2", "/home/ubuntu/.m2"
    config.vm.synced_folder "~/.m2", "/root/.m2"
    config.vm.synced_folder "~/.gnupg", "/home/ubuntu/.gnupg"
  end

  config.vm.provider "virtualbox" do |vb|
    vb.memory = "500"
  end
  
  # runs as root within the VM
  config.vm.provision "shell", inline: %q{
  
    set -e  # stop on error

    echo --- General OS installation
    apt-get update
    DEBIAN_FRONTEND=noninteractive apt-get upgrade -q -y    # grub upgrade warnings mess with the terminal
    apt-get -q -y install vim subversion ntp unattended-upgrades

    echo --- Install Java 8 \(OpenJDK\) and Maven
    apt-get -qy install openjdk-8-jdk maven

    echo --- Build software
    mvn -f /vagrant/pom.xml clean package
    echo 'mvn -f /vagrant/pom.xml clean package' >> ~vagrant/.bash_history
  }
  
  config.vm.provision "shell", run: "always", inline: %q{
  
    set -e  # stop on error
    
    echo ''
    echo '-----------------------------------------------------------------'
    echo 'After "vagrant ssh", use:'
    echo '  mvn -f /vagrant/pom.xml clean package'
    echo '-----------------------------------------------------------------'
    echo ''
  }
  
end
