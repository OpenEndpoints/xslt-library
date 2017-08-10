# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
  config.vm.hostname = "offerready-xslt-library"
  config.vm.box = "debian/contrib-jessie64"
  
  config.vm.provider "virtualbox" do |vb|
    vb.memory = "500"
  end
  
  # runs as root within the VM
  config.vm.provision "shell", inline: %q{
  
    set -e  # stop on error

    echo --- General OS installation
    echo "deb http://ftp.de.debian.org/debian jessie-backports main" >> /etc/apt/sources.list.d/backports.list
    apt-get update
    DEBIAN_FRONTEND=noninteractive apt-get upgrade -qy    # grub upgrade warnings mess with the terminal
    apt-get -qy install vim ant subversion ntp unattended-upgrades less

    echo --- Install Java 8 \(OpenJDK\)
    apt-get -qy install -t jessie-backports openjdk-8-jdk
    update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java

    echo --- Build software
    ant -f /vagrant/build.xml
  }
  
  config.vm.provision "shell", run: "always", inline: %q{
  
    set -e  # stop on error
    
    echo ''
    echo '-----------------------------------------------------------------'
    echo 'After "vagrant ssh", use:'
    echo '  ant -f /vagrant/build.xml'
    echo '-----------------------------------------------------------------'
    echo ''
  }
  
end
