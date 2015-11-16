function [isHomo] = IsHomogeneous(name)
    %get all lettes till first number 
    uniques = unique(name);
    res = regexp(name, uniques);
    if(numel(res) > 0)
       isHomo = true; 
    else
        if(2 == numel(name))
            isHomo = true;
        else
            isHomo = false;
        end
    end
    
end