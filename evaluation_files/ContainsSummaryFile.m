function result = ContainsSummaryFile(directoryname)
    if(exist(strcat(directoryname, '/summary.csv'), 'file') == 2)
        result = true;
    else
        result = false;
    end
end